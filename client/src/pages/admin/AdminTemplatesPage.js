import React, { Fragment as F } from 'react'
import cx from 'classnames'
import f from 'lodash'

import { Query } from 'react-apollo'
import gql from 'graphql-tag'

import t from '../../locale/translate'
import { Button, Collapsing, Tooltipped } from '../../components/Bootstrap'
import Icon from '../../components/Icons'
import Loading from '../../components/Loading'
import { MainWithSidebar } from '../../components/Layout'
import { formatCurrency } from '../../components/decorators'
import { ErrorPanel } from '../../components/Error'
import ImageThumbnail from '../../components/ImageThumbnail'
import CurrentUser from '../../containers/CurrentUserProvider'

// # DATA
//
const ADMIN_TEMPLATES_FRAGMENT = gql`
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
const ADMIN_TEMPLATES_PAGE_QUERY = gql`
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
  ${ADMIN_TEMPLATES_FRAGMENT}
`

export const ADMIN_UPDATE_TEMPLATES_MUTATION = gql`
  mutation adminUpdateTemplates($templates: [TemplateInput]) {
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
    supplier {
      id
    }
  }
`

export const formfoo = `
  article_name: String
  article_number: String
  category_id: ID!
  id: ID
  model: ID
  price_cents: Int
  supplier_id: ID
  to_delete: Boolean
`

// # PAGE
//
const AdminTemplates = () => (
  <CurrentUser>
    {me => (
      <Query query={ADMIN_TEMPLATES_PAGE_QUERY}>
        {({ loading, error, data }) => {
          if (loading) return <Loading />
          if (error) return <ErrorPanel error={error} data={data} />

          return (
            <MainWithSidebar>
              <h1 className="mb-4">
                <Icon.Templates /> {t('admin.templates.title')}
              </h1>

              <CategoriesList me={me} mainCategories={data.main_categories} />

              {window.isDebug && <pre>{JSON.stringify(data, 0, 2)}</pre>}
            </MainWithSidebar>
          )
        }}
      </Query>
    )}
  </CurrentUser>
)

export default AdminTemplates

const CategoriesList = ({ me, mainCategories }) => {
  const canEditCat = ({ id }) =>
    !!f.find(me.user.permissions.isInspectorForCategories, { id })

  const tableCols = [
    // { key: 'model', size: 2 },
    { key: 'article_name', size: 4 },
    { key: 'article_number', size: 3 },
    { key: 'price_cents', size: 2 },
    { key: 'supplier', size: 2 },
    { key: 'toDelete', size: 1 }
  ]

  return (
    <F>
      <ul className="list-unstyled">
        {mainCategories.map(
          mc =>
            f.any(mc.categories, canEditCat) && (
              <F key={mc.id}>
                <Collapsing
                  id={'mc' + mc.id}
                  canToggle={true}
                  // TODO: open closed
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
                        className={cx('card-header h4 cursor-pointer', {
                          'border-bottom-0': !isOpen
                        })}
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
                          {mc.categories.map(sc => {
                            const addButton = (
                              <Button
                                cls="m-0 p-0"
                                color="link"
                                onClick={e => alert(e)}
                              >
                                <Icon.PlusCircle color="success" />
                              </Button>
                            )
                            return (
                              canEditCat(sc) && (
                                <F key={sc.id}>
                                  <li className="list-group-item">
                                    <h3 className="h5 py-2">{sc.name}</h3>

                                    {!f.any(sc.templates) ? (
                                      <div>
                                        <p className="mb-2 small">
                                          {t(
                                            'admin.templates.category_has_no_templates'
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
                                                      `admin.templates.formfield.${key}`
                                                    )}
                                                  </th>
                                                )
                                              )}
                                            </tr>
                                          </thead>

                                          <tbody>
                                            {sc.templates.map(tpl => (
                                              <TemplateRow
                                                key={tpl.id}
                                                cols={tableCols}
                                                {...tpl}
                                              />
                                            ))}
                                          </tbody>

                                          {/* TODO: add templates
                                        <tfoot>
                                          <tr>
                                            <td className="col-12">
                                              {addButton}
                                            </td>
                                          </tr>
                                        </tfoot> */}
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
    </F>
  )
}

const TemplateRow = ({ cols, ...tpl }) => (
  <tr className="row no-gutters">
    {cols.map(({ key, size }, i) => (
      <td key={i} className={`col-${size}`}>
        {key === 'price_cents' ? (
          <samp>{formatCurrency(tpl.price_cents)}</samp>
        ) : key === 'toDelete' ? (
          ''
        ) : (
          tpl[key]
        )}
      </td>
    ))}
  </tr>
)
