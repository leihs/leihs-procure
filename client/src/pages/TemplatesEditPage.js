import React, { Fragment as F, useEffect, useRef, useState } from 'react'
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
    is_archived
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

    let templates = f
      .flatMap(onlyEditableCats, sc =>
        f.flatMap(sc.templates, tpl => ({
          id: tpl.id,
          article_name: tpl.article_name,
          article_number: tpl.article_number,
          price_cents: tpl.price_cents,
          to_delete: tpl.toDelete,
          is_archived: tpl.toArchive,
          category_id: sc.id,
          supplier_name: f.presence(tpl.supplier_name) || null

          // ...(!!tpl.model && { model: tpl.model.id }),
          // ...(!!tpl.supplier && { model: tpl.supplier.id }),
        }))
      )
      .filter(
        template => !template.id || template.to_delete || template.is_archived
      )

    // const templatesToUpdate = Array.from(
    //   document.querySelectorAll("[data-remove='true'], [data-archive]")
    // )
    //
    // // const templatesToUpdate = [...templatesToAdd, ...templatesToEdit]
    //
    // onlyEditableCats.forEach(category => {
    //   templatesToUpdate.forEach(templateToUpdate => {
    //     const updateId = templateToUpdate.getAttribute('data-id')
    //
    //     const toDelete = JSON.parse(
    //       templateToUpdate.getAttribute('data-remove')
    //     )
    //     const toArchive = JSON.parse(
    //       templateToUpdate.getAttribute('data-archive')
    //     )
    //
    //     const data = category.templates.find(
    //       template => template.id === updateId
    //     )
    //
    //     if (data) {
    //       toDelete ? (data.to_delete = toDelete) : delete data.to_delete
    //
    //       data.is_archived = toArchive
    //       data.category_id = category.id
    //       delete data.__typename
    //       delete data.model
    //       delete data.supplier
    //       delete data.can_delete
    //       delete data.price_currency
    //       templates.push(data)
    //     }
    //   })
    // })

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
                                                  <Table
                                                    tableCols={tableCols}
                                                    addButton={addButton}
                                                    mci={mci}
                                                    sci={sci}
                                                    sc={sc}
                                                    formPropsFor={formPropsFor}
                                                  ></Table>
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

function Table({ children, tableCols, addButton, mci, sci, sc, formPropsFor }) {
  const [showArchived, setShowArchived] = useState(false)

  return (
    <>
      <div class="d-flex">
        <h3 className="h5 py-2">{sc.name}</h3>
        <div className="custom-control custom-switch align-self-center ml-auto">
          <input
            type="checkbox"
            id="customSwitch1"
            className="custom-control-input"
            checked={showArchived}
            onChange={e => setShowArchived(e.target.checked)}
          />
          <label className="custom-control-label" htmlFor="customSwitch1">
            Show Archived
          </label>
        </div>
      </div>
      <table className="table table-sm table-hover">
        <thead className="small">
          <tr className="row no-gutters">
            {f.map(tableCols, ({ key, size }) => (
              <th key={key} className={`col-${size}`}>
                {t(`templates.formfield.${key}`)}
              </th>
            ))}
          </tr>
        </thead>

        <tbody>
          {sc.templates.map((tpl, i) =>
            !tpl.is_archived || showArchived ? (
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
            ) : null
          )}
        </tbody>

        <tfoot>
          <tr>
            <td className="col-12">{addButton}</td>
          </tr>
        </tfoot>
      </table>
    </>
  )
}

const TemplateRow = ({ cols, onClick, formPropsFor, ...tpl }) => {
  const rowRef = useRef(null)

  const isEditing = true // TODO: edit on click
  const inputFieldCls = cx({
    'text-strike bg-danger-light': tpl?.toDelete || false
  })

  console.debug(tpl)

  // disabling all inputs by hand, since input componets are nested quite alot...
  useEffect(() => {
    if (!tpl.can_delete && tpl.id) {
      const inputs = rowRef.current.querySelectorAll('input')
      inputs.forEach(
        input => input.type !== 'checkbox' && input.setAttribute('disabled', '')
      )
    }
  })

  return (
    <tr
      ref={rowRef}
      data-id={tpl.id}
      className="row no-gutters templates"
      onClick={onClick}
    >
      {cols.map(({ key, size, required }, i) => (
        <td
          key={i}
          className={cx('text-center pr-1', `col-${size}`, {
            'was-validated': isEditing && !tpl.id
          })}
        >
          {isEditing ? (
            key === 'toDelete' ? (
              tpl.can_delete ? (
                <Let toDelete={formPropsFor('toDelete')}>
                  {({ toDelete }) => (
                    <label id={`btn_del_${tpl.id}`} className="pt-1">
                      <Icon.Trash
                        size="lg"
                        className={cx(
                          tpl?.toDelete ? 'text-danger' : 'text-dark'
                        )}
                      />
                      <input
                        type="checkbox"
                        className="sr-only"
                        name="delete"
                        checked={tpl?.toDelete || false}
                        onChange={e =>
                          toDelete.onChange({
                            target: {
                              name: toDelete.name,
                              checked: e.target.checked,
                              value: e.target.checked
                            }
                          })
                        }
                      />
                    </label>
                  )}
                </Let>
              ) : tpl.is_archived !== undefined ? (
                <Let toArchive={formPropsFor('toArchive')}>
                  {({ toArchive }) => (
                    <label id={`btn_del_${tpl.id}`} className="pt-1">
                      <Icon.Archive
                        size="lg"
                        className={cx(
                          tpl?.is_archived || tpl?.toArchive
                            ? 'text-warning'
                            : 'text-dark'
                        )}
                      />
                      <input
                        type="checkbox"
                        className="sr-only"
                        name="archive"
                        checked={tpl?.toArchive || false}
                        onChange={e =>
                          toArchive.onChange({
                            target: {
                              name: toArchive.name,
                              checked: e.target.checked,
                              value: e.target.checked
                            }
                          })
                        }
                      />
                    </label>
                  )}
                </Let>
              ) : (
                <></>
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
