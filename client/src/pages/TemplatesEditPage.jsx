import React, { Fragment as F, useEffect, useRef, useState } from 'react'
import cx from 'classnames'
import f from 'lodash'

import { Query, Mutation } from '@apollo/client/react/components'
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
import { UncontrolledTooltip } from 'reactstrap'

// # DATA
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
          id
          article_name
          article_number
          model {
            id
            product
            version
          }
          requests_count
          is_archived
          price_cents
          price_currency
          supplier_name
          supplier {
            id
          }
        }
      }
    }
  }
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
    const onlyEditableCats = mainCategories
      .flatMap(cat => cat['categories'])
      .filter(
        sc =>
          !!me.user.permissions.isInspectorForCategories.find(
            el => el.id === sc.id
          )
      )

    let templates = onlyEditableCats.flatMap(sc =>
      sc.templates.flatMap(tpl => ({
        id: tpl.id,
        article_name: tpl.article_name,
        article_number: tpl.article_number,
        price_cents: tpl.price_cents,
        is_archived: tpl.is_archived,
        category_id: sc.id,
        supplier_name: f.presence(tpl.supplier_name) || null,
        ...(!!tpl.toDelete && { to_delete: tpl.toDelete })

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
    { key: 'link', size: 1 },
    { key: 'article_name', size: 3, required: true },
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
  const switchRef = useRef(null)
  const hasArchivedEntry = sc.templates.find(el => el.is_archived === true)
  const [showArchived, setShowArchived] = useState(false)

  // useEffect(() => {
  //   !!hasArchivedEntry
  //     ? switchRef.current.removeAttribute('disabled', '')
  //     : switchRef.current.setAttribute('disabled', '')
  // })

  return (
    <>
      <div className="d-flex">
        {/* Table Heading  */}
        <h3 className="h5 py-2">{sc.name}</h3>

        {/*  Switch Archived Entries */}
        <div
          className={cx(
            'custom-control custom-switch align-self-center ml-auto',
            hasArchivedEntry ? 'visible' : 'invisible'
          )}
        >
          <input
            ref={switchRef}
            type="checkbox"
            id={'archiveSwitch' + mci}
            className={cx('custom-control-input')}
            checked={showArchived}
            onChange={e => setShowArchived(e.target.checked)}
          />
          <label
            className="custom-control-label"
            htmlFor={'archiveSwitch' + mci}
          >
            {t(`templates.tooltips.show_archived`)}
          </label>
        </div>
      </div>

      {sc.templates.length === 0 && (
        <p className="mb-2 small">{t('templates.category_has_no_templates')}</p>
      )}

      <table className="table table-sm table-hover">
        {sc.templates.length > 0 && (
          <thead className="small">
            <tr className="row no-gutters">
              {f.map(tableCols, ({ key, size }) => (
                <th key={key} className={`col-${size}`}>
                  {t(`templates.formfield.${key}`)}
                </th>
              ))}
            </tr>
          </thead>
        )}

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
  const toDeleteRef = useRef(null)

  const isEditing = true // TODO: edit on click
  const inputFieldCls = cx({
    'text-strike bg-danger-light': tpl?.toDelete || false
  })

  // disabling all inputs by hand, since input componets are nested quite alot...
  useEffect(() => {
    if (tpl.requests_count && tpl.id) {
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
              !tpl.requests_count && !tpl.is_archived ? (
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
                        ref={toDeleteRef}
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
                      <UncontrolledTooltip
                        placement="left"
                        target={`btn_del_${tpl.id}`}
                      >
                        {t(`templates.tooltips.delete_template`)}
                      </UncontrolledTooltip>
                    </label>
                  )}
                </Let>
              ) : (
                <Let is_archived={formPropsFor('is_archived')}>
                  {({ is_archived }) => (
                    <label id={`btn_archive_${tpl.id}`} className="pt-1 ">
                      <Icon.Archive
                        size="lg"
                        className={cx(
                          tpl?.is_archived ? 'text-warning' : 'text-dark'
                        )}
                      />
                      <input
                        type="checkbox"
                        className="sr-only"
                        name="archive"
                        checked={!!tpl.is_archived}
                        onChange={e =>
                          is_archived.onChange({
                            target: {
                              name: is_archived.name,
                              checked: e.target.checked,
                              value: e.target.checked
                            }
                          })
                        }
                      />
                      <UncontrolledTooltip
                        placement="left"
                        target={`btn_archive_${tpl.id}`}
                      >
                        {tpl.is_archived ? (
                          <>{t(`templates.tooltips.unarchive_template`)}</>
                        ) : (
                          <>{t(`templates.tooltips.archive_template`)}</>
                        )}
                      </UncontrolledTooltip>
                    </label>
                  )}
                </Let>
              )
            ) : key === 'link' ? (
              <div className="d-flex h-100 align-items-center justify-content-center">
                <p className="h3 mb-0 mr-2">{tpl.requests_count}</p>
                <Icon.Link id={`link_${tpl.id}`} size="lg" />
                <UncontrolledTooltip
                  placement="right"
                  target={`link_${tpl.id}`}
                >
                  {tpl.requests_count}{' '}
                  {t(`templates.tooltips.template_requests`)}
                </UncontrolledTooltip>
              </div>
            ) : key === 'price_cents' ? (
              <Let priceField={formPropsFor('price_cents')}>
                {({ priceField }) => (
                  <InputText
                    cls={inputFieldCls}
                    type="number"
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
