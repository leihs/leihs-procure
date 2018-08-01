import React, { Fragment as F } from 'react'
import f from 'lodash'
import cx from 'classnames'
import { Query } from 'react-apollo'
import gql from 'graphql-tag'
// import qs from 'qs'

// import * as CONSTANTS from '../constants'
import * as Fragments from '../graphql-fragments'
// import t from '../locale/translate'
import Icon from '../components/Icons'
import {
  Row,
  Col,
  Badge,
  Collapsing,
  // FilePicker,
  FormGroup,
  // FormField,
  Select,
  // ButtonRadio,
  StatefulForm,
  // ButtonDropdown,
  // DropdownToggle,
  // DropdownMenu,
  // DropdownItem,
  RouteParams
} from '../components/Bootstrap'
import { MainWithSidebar } from '../components/Layout'
import Loading from '../components/Loading'
import { ErrorPanel } from '../components/Error'
import { formatCurrency } from '../components/decorators'
import ImageThumbnail from '../components/ImageThumbnail'

import RequestForm from '../components/RequestForm'

const NEW_REQUEST_PRESELECTION_QUERY = gql`
  query newRequestPreselectionQuery {
    budget_periods {
      id
      name
      inspection_start_date
      end_date
    }
    main_categories {
      id
      name
      image_url
      categories {
        id
        name
      }
    }
    templates {
      id
      category {
        id
      }
      article_name
      article_number
      model {
        id
        product
      }
      price_cents
      price_currency
      supplier {
        id
      }
    }
  }
`

const NEW_REQUEST_QUERY = gql`
  query newRequestQuery($budgetPeriod: ID, $category: ID, $template: ID) {
    new_request(
      budget_period: $budgetPeriod
      category: $category
      template: $template
    ) {
      ...RequestFieldsForEdit
    }
  }

  ${Fragments.RequestFieldsForEdit}
`

const FAKE_DATA = {
  FROM_CATEGORY: {
    new_request: {
      template: null,
      category: {
        value: {
          id: '5304d73e-28ff-4ee8-9fdd-f032fa0711c1',
          name: 'Stuff'
        }
      },
      budget_period: {
        value: {
          id: '8a9af029-86a5-4d5e-bd05-278f94b89cf3'
        }
      },
      article_name: {
        default: null,
        value: null,
        required: true,
        read: true,
        write: true
      },
      receiver: {
        default: null,
        value: null,
        required: false,
        read: true,
        write: true
      },
      organization: {
        value: null
      },
      price_cents: {
        default: null,
        value: null,
        required: true,
        read: true,
        write: true
      },
      price_currency: {
        default: 'CHF',
        value: 'CHF',
        required: true,
        read: true,
        write: false
      },
      requested_quantity: {
        default: null,
        value: null,
        required: true,
        read: true,
        write: true
      },
      approved_quantity: {
        default: null,
        value: null,
        required: false,
        read: true,
        write: true
      },
      order_quantity: {
        default: null,
        value: null,
        required: false,
        read: true,
        write: true
      },
      replacement: {
        default: null,
        value: null,
        required: true,
        read: true,
        write: true
      },
      priority: {
        default: 'NORMAL',
        value: 'NORMAL',
        required: true,
        read: true,
        write: true
      },
      state: {
        default: 'new',
        value: 'new',
        required: true,
        read: true,
        write: false
      },
      supplier: {
        default: null,
        read: true,
        required: false,
        write: true,
        value: null
      },
      inspector_priority: {
        default: 'MEDIUM',
        value: 'MEDIUM',
        required: false,
        read: true,
        write: true
      },
      article_number: {
        default: null,
        value: null,
        required: false,
        read: true,
        write: true
      },
      motivation: {
        default: null,
        value: null,
        required: true,
        read: true,
        write: true
      },
      room: {
        default: null,
        value: null,
        required: true,
        read: true,
        write: true
      },
      inspection_comment: {
        default: null,
        value: null,
        required: false,
        read: true,
        write: true
      },
      accounting_type: {
        default: 'aquisition',
        value: 'aquisition',
        required: true,
        read: true,
        write: true
      },
      cost_center: {
        default: '12345',
        value: '12345',
        required: false,
        read: true,
        write: false
      },
      procurement_account: {
        default: null,
        value: null,
        required: false,
        read: true,
        write: false
      },
      internal_order_number: {
        default: null,
        value: null,
        required: false,
        read: true,
        write: true
      }
    }
  },
  FROM_TEMPLATE: {
    new_request: {
      template: {
        id: '5badf5e7-d8d1-43f4-bbf4-908ca6e5c548',
        article_name: 'iPad mini'
      },
      category: {
        value: {
          id: '8b8d8419-35c0-45cf-8f85-1c25b84e9058',
          name: 'Stuff'
        }
      },
      budget_period: {
        value: {
          id: '8a1137a2-5c27-4825-b12f-428bbc192a7c'
        }
      },
      article_name: {
        default: 'iPad mini',
        value: 'iPad mini',
        required: true,
        read: true,
        write: false
      },
      receiver: {
        default: null,
        value: null,
        required: false,
        read: true,
        write: true
      },
      organization: {
        value: null
      },
      price_cents: {
        default: 45000,
        value: 45000,
        required: true,
        read: true,
        write: false
      },
      price_currency: {
        default: 'CHF',
        value: 'CHF',
        required: true,
        read: true,
        write: false
      },
      requested_quantity: {
        default: 1,
        value: 1,
        required: true,
        read: true,
        write: true
      },
      approved_quantity: {
        default: null,
        value: null,
        required: false,
        read: true,
        write: true
      },
      order_quantity: {
        default: null,
        value: null,
        required: true,
        read: true,
        write: true
      },
      replacement: {
        default: null,
        value: null,
        required: false,
        read: true,
        write: true
      },
      priority: {
        default: 'NORMAL',
        value: 'NORMAL',
        required: false,
        read: true,
        write: true
      },
      state: {
        default: 'new',
        value: 'new',
        required: false,
        read: true,
        write: false
      },
      supplier: {
        default: {
          id: '97b27eec-92c2-425b-8631-8d8dc9deb0f7',
          name: 'Baba Kwest AG'
        },
        value: {
          id: '97b27eec-92c2-425b-8631-8d8dc9deb0f7',
          name: 'Baba Kwest AG'
        },
        required: false,
        read: true,
        write: true
      },
      inspector_priority: {
        default: 'MEDIUM',
        value: 'MEDIUM',
        required: false,
        read: true,
        write: true
      },
      article_number: {
        default: null,
        required: false,
        value: null,
        read: true,
        write: true
      },
      motivation: {
        default: null,
        value: null,
        required: true,
        read: true,
        write: true
      },
      room: {
        default: null,
        value: null,
        required: true,
        read: true,
        write: true
      },
      inspection_comment: {
        default: null,
        value: null,
        required: false,
        read: true,
        write: true
      },
      accounting_type: {
        default: 'aquisition',
        value: 'aquisition',
        required: false,
        read: true,
        write: true
      },
      cost_center: {
        default: '12345',
        value: '12345',
        required: false,
        read: true,
        write: false
      },
      procurement_account: {
        default: null,
        value: null,
        required: false,
        read: true,
        write: false
      },
      internal_order_number: {
        default: null,
        value: null,
        required: false,
        read: true,
        write: true
      }
    }
  }
}

// TODO: form-selection-to-params
// const updateQueryParams = ({ fields, params, location }) => {
//   const formParams = {
//     ...f.pick(fields, ['budget_period', 'category']),
//     requester: f.get(fields, 'requester.id')
//   }
//   return {
//     ...location,
//     search: '?' + qs.stringify({ ...params, ...formParams })
//   }
// }

class RequestNewPage extends React.Component {
  // TODO: form-selection-to-params
  //      - apply url params as state.initalSelection
  //      - needs query-user-by-id so we get the full object w/ name from uuid.
  // constructor() {
  //   this.state = {initalSelection: …}
  // }
  render() {
    return (
      <RouteParams>
        {({ params, location, history }) => (
          <MainWithSidebar>
            <h1>Antrag erstellen</h1>

            <Query
              query={NEW_REQUEST_PRESELECTION_QUERY}
              networkPolicy="cache-then-network"
            >
              {({ loading, error, data }) => {
                if (loading) return <Loading />
                if (error) return <ErrorPanel error={error} data={data} />

                return (
                  <NewRequestPreselection
                    data={data}
                    // TODO: form-selection-to-params
                    // onSelectionChange={fields =>
                    //   history.replace(
                    //     updateQueryParams({ params, location, fields })
                    //   )
                    // }
                    onSelectionChange={() => {}}
                  />
                )
              }}
            </Query>
          </MainWithSidebar>
        )}
      </RouteParams>
    )
  }
}

export default RequestNewPage

const NewRequestPreselection = ({ data, onSelectionChange }) => {
  const budPeriods = f
    .sortBy(data.budget_periods, 'end_date')
    .filter(bp => new Date(bp.inspection_start_date).getTime() > Date.now())
    .map(bp => ({
      value: bp.id,
      label: `${bp.name} – Antragsphase bis ${new Date(
        bp.inspection_start_date
      ).toLocaleDateString()}`
    }))

  const CatWithMainCat = catId => {
    const mc = f.find(data.main_categories, {
      categories: [{ id: catId }]
    })
    const sc = f.find(mc.categories, { id: catId })
    return { ...sc, main_category: { ...mc, categories: undefined } }
  }

  return (
    <StatefulForm
      idPrefix={`request_new`}
      values={{ budget_period: budPeriods[0].value }}
      onChange={fields => onSelectionChange(fields)}
    >
      {({ fields, setValue, getValue, formPropsFor, ...formHelpers }) => {
        const selectedCategory = fields.category
        const selectedTemplate = f.find(data.templates, { id: fields.template })
        const hasPreselected = !!(selectedTemplate || selectedCategory)

        return (
          <F>
            <form
              onSubmit={e => {
                e.preventDefault()
                window.alert('TODO!')
              }}
            >
              <FormGroup label="Budgetperiode" className="form-control-lg p-0">
                <Select
                  {...formPropsFor('budget_period')}
                  className="form-control-lg"
                  required
                  emptyOption={false}
                  options={budPeriods}
                />
              </FormGroup>

              <FormGroup
                label="Kategorie & Vorlage"
                className="form-control-lg p-0"
              >
                {hasPreselected ? (
                  selectedCategory ? (
                    <Row>
                      <Col sm>
                        <SelectedCategory
                          cat={CatWithMainCat(selectedCategory)}
                        />
                      </Col>
                      <Col sm>
                        <SelectionCard>Keine Vorlage</SelectionCard>
                      </Col>
                    </Row>
                  ) : (
                    <Row>
                      <Col sm>
                        <SelectedCategory
                          cat={CatWithMainCat(selectedTemplate.category.id)}
                        />
                      </Col>
                      <Col sm>
                        <SelectionCard>
                          <Icon.Templates spaced />
                          {selectedTemplate.article_name}
                        </SelectionCard>
                      </Col>
                    </Row>
                  )
                ) : null}

                {!hasPreselected && (
                  <CategoriesTemplatesTree
                    main_categories={data.main_categories}
                    templates={data.templates}
                    onSelectCategory={t => setValue('category', t.id)}
                    onSelectTemplate={t => setValue('template', t.id)}
                  />
                )}
              </FormGroup>
            </form>

            {hasPreselected && (
              <NewRequestForm
                category={selectedCategory}
                template={selectedTemplate}
                onCancel={e => {
                  setValue('template', null)
                  setValue('category', null)
                }}
                // NOTE: reset form if preselection changes
                key={['budget_period', 'category', 'template']
                  .map(k => String([fields[k]]))
                  .join()}
              />
            )}
          </F>
        )
      }}
    </StatefulForm>
  )
}

const NewRequestForm = ({ template, category, onCancel }) => (
  <F>
    <Query
      query={NEW_REQUEST_QUERY}
      networkPolicy="network-only"
      skip={!!FAKE_DATA}
    >
      {() => {
        /* {({ loading, error, data }) => {
        if (loading) return <Loading />
        if (error) return <ErrorPanel error={error} data={data} /> */

        const data = FAKE_DATA[template ? 'FROM_TEMPLATE' : 'FROM_CATEGORY']

        const request = { ...data.new_request }

        return (
          <F>
            <hr className="my-4" />
            <RequestForm
              request={request}
              categories={data.main_categories}
              budgetPeriods={data.budget_periods}
              onCancel={onCancel}
              onSubmit={fields => window.alert(JSON.stringify(fields, 0, 2))}
            />
          </F>
        )
      }}
    </Query>
  </F>
)

const CategoriesTemplatesTree = ({
  main_categories,
  templates,
  onSelectCategory,
  onSelectTemplate
}) => (
  <F>
    <ul className="list-unstyled">
      {main_categories.map(mc => (
        <F key={mc.id}>
          <Collapsing id={'mc' + mc.id} canToggle={true} startOpen={false}>
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
                    {mc.categories.map(sc => (
                      <F key={sc.id}>
                        <li className="card list-group-item p-0">
                          <h4 className="card-header h6">{sc.name}</h4>
                          <div className="list-group list-group-flush">
                            {f
                              .filter(templates, {
                                category: { id: sc.id }
                              })
                              .map(t => (
                                <F key={t.id}>
                                  <button
                                    className="list-group-item list-group-item-action cursor-pointer"
                                    onClick={e => {
                                      e.preventDefault
                                      onSelectTemplate(t)
                                    }}
                                  >
                                    <Icon.PlusCircle
                                      color="success"
                                      className="mr-3"
                                      size="lg"
                                    />
                                    <Icon.Templates /> {t.article_name}{' '}
                                    <Badge secondary>
                                      {formatCurrency(t.price_cents)}
                                    </Badge>
                                  </button>
                                </F>
                              ))}
                            <button
                              className="list-group-item list-group-item-action cursor-pointer"
                              onClick={e => {
                                e.preventDefault
                                onSelectCategory(sc)
                              }}
                            >
                              <Icon.PlusCircle
                                color="success"
                                className="mr-3"
                              />
                              {'Ohne Vorlage erstellen'}
                            </button>
                          </div>
                        </li>
                      </F>
                    ))}
                  </ul>
                )}
              </li>
            )}
          </Collapsing>
        </F>
      ))}
    </ul>
  </F>
)

const SelectionCard = ({ children }) => (
  <div className="card">
    <div className="card-body px-3 py-2">{children}</div>
  </div>
)

const SelectedCategory = ({ cat }) => (
  <SelectionCard>
    <Icon.Categories spaced="2" />
    {cat.main_category.name}
    <Icon.CaretRight />
    {cat.name}
  </SelectionCard>
)
