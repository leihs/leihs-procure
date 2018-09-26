import React, { Fragment as F } from 'react'
import cx from 'classnames'
import f from 'lodash'

import { Query, Mutation } from 'react-apollo'
import gql from 'graphql-tag'

import t from '../locale/translate'
import {
  Button,
  InputText,
  Collapsing,
  StatefulForm
} from '../components/Bootstrap'
import Icon from '../components/Icons'
import Loading from '../components/Loading'
import { MainWithSidebar } from '../components/Layout'
import { formatCurrency } from '../components/decorators'
import { ErrorPanel } from '../components/Error'
import ImageThumbnail from '../components/ImageThumbnail'
import { mutationErrorHandler } from '../apollo-client'
import CurrentUser from '../containers/CurrentUserProvider'

// # DATA
//
const TEMPLATES_FRAGMENT = gql`
  fragment TemplateProps on Template {
    id
    can_delete
    article_name
    article_number
    model {
      id
      product
      version
    }
    price_cents
    price_currency
    supplier_name
    supplier {
      id
    }
  }
`
const TEMPLATES_PAGE_QUERY = gql`
  query adminTemplates {
    main_categories {
      id
      name
      image_url
      categories {
        id
        name
        templates {
          ...TemplateProps
        }
      }
    }
  }
  ${TEMPLATES_FRAGMENT}
`

const UPDATE_TEMPLATES_MUTATION = gql`
  mutation adminUpdateTemplates($templates: [TemplateInput]) {
    update_templates(input_data: $templates) {
      id
    }
  }
`

const updateTemplates = {
  mutation: {
    mutation: UPDATE_TEMPLATES_MUTATION,
    onError: mutationErrorHandler,
    update: (cache, { data: { main_categories } }) => {
      // TODO: better update handling
      window.location.reload()
    }
  },
  doUpdate: (mutate, me, { mainCategories }) => {
    const onlyEditableCats = f.filter(
      f.flatMap(mainCategories, 'categories'),
      sc =>
        !!f.find(me.user.permissions.isInspectorForCategories, { id: sc.id })
    )
    const templates = f.flatMap(onlyEditableCats, sc =>
      f.flatMap(sc.templates, tpl => ({
        id: tpl.id,
        article_name: tpl.article_name,
        article_number: tpl.article_number,
        price_cents: tpl.price_cents,
        to_delete: tpl.toDelete,

        category_id: sc.id,
        // TODO: model/supplier
        supplier_name: f.presence(tpl.supplier_name) || null
        // ...(!!tpl.model && { model: tpl.model.id }),
        // ...(!!tpl.supplier && { model: tpl.supplier.id }),
      }))
    )

    mutate({
      variables: { templates }
    })
  }
}

// # PAGE
//
class AdminTemplates extends React.Component {
  state = { formKey: 1 }
  render() {
    return (
      <CurrentUser>
        {me => (
          <Mutation
            {...updateTemplates.mutation}
            onCompleted={() => this.setState({ formKey: Date.now() })}
          >
            {(mutate, info) => (
              <Query query={TEMPLATES_PAGE_QUERY}>
                {({ loading, error, data }) => {
                  if (loading) return <Loading />
                  if (error) return <ErrorPanel error={error} data={data} />

                  return (
                    <MainWithSidebar>
                      <h1 className="mb-4">
                        <Icon.Templates /> {t('templates.title_edit')}
                      </h1>

                      <CategoriesList
                        me={me}
                        mainCategories={data.main_categories}
                        formKey={this.state.formKey}
                        onSubmit={d => updateTemplates.doUpdate(mutate, me, d)}
                      />

                      {window.isDebug && (
                        <pre>
                          <b>API Data</b>
                          <br />
                          {JSON.stringify(data, 0, 2)}
                        </pre>
                      )}
                    </MainWithSidebar>
                  )
                }}
              </Query>
            )}
          </Mutation>
        )}
      </CurrentUser>
    )
  }
}

export default AdminTemplates

const CategoriesList = ({ me, mainCategories, onSubmit, formKey }) => {
  const canEditCat = ({ id }) =>
    !!f.find(me.user.permissions.isInspectorForCategories, { id })

  const tableCols = [
    { key: 'article_name', size: 4, required: true },
    { key: 'article_number', size: 3 },
    { key: 'price_cents', size: 2, required: true },
    { key: 'supplier_name', size: 2 },
    { key: 'toDelete', size: 1 }
  ]

  return (
    <F>
      <StatefulForm
        formKey={formKey}
        idPrefix="templates"
        values={{ mainCategories }}
      >
        {({ fields, formPropsFor, setValue }) => {
          const onAddTemplate = ({ id }) => {
            const mci = f.findIndex(fields.mainCategories, {
              categories: [{ id: id }]
            })
            const mc = fields.mainCategories[mci]
            const sci = f.findIndex(mc.categories, { id: id })
            setValue(
              `mainCategories.${mci}.categories.${sci}.templates`,
              mc.categories[sci].templates.concat({})
            )
          }
          return (
            <F>
              <form
                onSubmit={e => {
                  e.preventDefault()
                  onSubmit(fields)
                }}
              >
                <ul className="list-unstyled">
                  {fields.mainCategories.map(
                    (mc, mci) =>
                      f.any(mc.categories, canEditCat) && (
                        <F key={mc.id}>
                          <Collapsing
                            id={'mc' + mc.id}
                            canToggle={true}
                            startOpen={mainCategories.length === 1}
                          >
                            {({
                              isOpen,
                              canToggle,
                              toggleOpen,
                              togglerProps,
                              collapsedProps,
                              Caret
                            }) => (
                              <li className="card mb-3">
                                <h3
                                  className={cx(
                                    'card-header h4 cursor-pointer',
                                    {
                                      'border-bottom-0': !isOpen
                                    }
                                  )}
                                  {...togglerProps}
                                >
                                  <Caret spaced />
                                  {!!mc.image_url && (
                                    <ImageThumbnail
                                      className="border-0 bg-transparent"
                                      imageUrl={mc.image_url}
                                    />
                                  )}
                                  {mc.name}
                                </h3>
                                {isOpen && (
                                  <ul className="list-group list-group-flush">
                                    {mc.categories.map((sc, sci) => {
                                      const addButton = (
                                        <Button
                                          cls="m-0 p-0"
                                          color="link"
                                          onClick={e => onAddTemplate(sc)}
                                        >
                                          <Icon.PlusCircle color="success" />
                                        </Button>
                                      )
                                      return (
                                        canEditCat(sc) && (
                                          <F key={sc.id}>
                                            <li className="list-group-item">
                                              <h3 className="h5 py-2">
                                                {sc.name}
                                              </h3>

                                              {!f.any(sc.templates) ? (
                                                <div>
                                                  <p className="mb-2 small">
                                                    {t(
                                                      'templates.category_has_no_templates'
                                                    )}
                                                  </p>
                                                  {addButton}
                                                </div>
                                              ) : (
                                                <div className="table-responsive">
                                                  <table className="table table-sm table-hover">
                                                    <thead className="small">
                                                      <tr className="row no-gutters">
                                                        {f.map(
                                                          tableCols,
                                                          ({ key, size }) => (
                                                            <th
                                                              key={key}
                                                              className={`col-${size}`}
                                                            >
                                                              {t(
                                                                `templates.formfield.${key}`
                                                              )}
                                                            </th>
                                                          )
                                                        )}
                                                      </tr>
                                                    </thead>

                                                    <tbody>
                                                      {sc.templates.map(
                                                        (tpl, i) => (
                                                          <TemplateRow
                                                            key={tpl.id || i}
                                                            cols={tableCols}
                                                            formPropsFor={key =>
                                                              formPropsFor(
                                                                `mainCategories.${mci}.categories.${sci}.templates.${i}.${key}`
                                                              )
                                                            }
                                                            {...tpl}
                                                          />
                                                        )
                                                      )}
                                                    </tbody>

                                                    <tfoot>
                                                      <tr>
                                                        <td className="col-12">
                                                          {addButton}
                                                        </td>
                                                      </tr>
                                                    </tfoot>
                                                  </table>
                                                </div>
                                              )}
                                            </li>
                                          </F>
                                        )
                                      )
                                    })}
                                  </ul>
                                )}
                              </li>
                            )}
                          </Collapsing>
                        </F>
                      )
                  )}
                </ul>
                <Button color="primary" type="submit">
                  <Icon.Checkmark /> <span>{t('form_btn_save')}</span>
                </Button>
              </form>
              {window.isDebug && <pre>{JSON.stringify(fields, 0, 2)}</pre>}
            </F>
          )
        }}
      </StatefulForm>
    </F>
  )
}

const TemplateRow = ({ cols, onClick, formPropsFor, ...tpl }) => {
  const isEditing = true // TODO: edit on click
  const inputFieldCls = cx({ 'text-strike bg-danger-light': tpl.toDelete })
  return (
    <tr className="row no-gutters" onClick={onClick}>
      {cols.map(({ key, size, required }, i) => (
        <td
          key={i}
          className={cx('text-center pr-1', `col-${size}`, {
            'was-validated': isEditing && !tpl.id
          })}
        >
          {isEditing ? (
            key === 'toDelete' ? (
              tpl.can_delete && (
                <Let field={formPropsFor('toDelete')}>
                  {({ field }) => (
                    <label id={`btn_del_${tpl.id}`} className="pt-1">
                      <Icon.Trash
                        size="lg"
                        className={cx(
                          tpl.toDelete ? 'text-dark' : 'text-danger'
                        )}
                      />
                      <input
                        type="checkbox"
                        className="sr-only"
                        name={field.name}
                        checked={!!field.value}
                        onChange={field.onChange}
                      />
                    </label>
                  )}
                </Let>
              )
            ) : key === 'price_cents' ? (
              <Let priceField={formPropsFor('price_cents')}>
                {({ priceField }) => (
                  <InputText
                    cls={inputFieldCls}
                    required={required}
                    value={tpl.price_cents / 100 || ''}
                    onChange={e =>
                      priceField.onChange({
                        target: {
                          name: priceField.name,
                          value: !e.target.value ? '' : e.target.value * 100
                        }
                      })
                    }
                  />
                )}
              </Let>
            ) : (
              <InputText
                cls={inputFieldCls}
                required={required}
                {...formPropsFor(key)}
              />
            )
          ) : key === 'price_cents' ? (
            <samp>{formatCurrency(tpl.price_cents)}</samp>
          ) : key === 'toDelete' ? (
            false
          ) : (
            tpl[key]
          )}
        </td>
      ))}
    </tr>
  )
}

const Let = ({ children, ...props }) => children(props)
