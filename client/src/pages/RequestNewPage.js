import React, { Fragment as F } from 'react'
import f from 'lodash'
import cx from 'classnames'
import { Query, Mutation } from 'react-apollo'
import gql from 'graphql-tag'
import qs from 'qs'

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
import { RequestLineClosed } from '../components/RequestLine'

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
  query newRequestQuery($budgetPeriod: ID!, $category: ID, $template: ID) {
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

const CREATE_REQUEST_MUTATION = gql`
  mutation createRequest($requestData: NewRequestInput) {
    create_request(input_data: $requestData) {
      ...RequestFieldsForEdit
    }
  }
  ${Fragments.RequestFieldsForEdit}
`

const valueIfWritable = (fields, requestData, reqKey, fieldKey) => {
  fieldKey = fieldKey || reqKey

  if (!f.get(requestData, reqKey)) {
    // eslint-disable-next-line no-debugger
    debugger
  }

  if (f.get(requestData, reqKey).write) {
    return { [fieldKey]: f.get(fields, fieldKey) }
  }
}

const boolify = (key, val) =>
  !val ? false : !val[key] ? null : val[key] === key

const updateRequestFromFields = (mutate, request, fields, preselection) => {
  const requestData = {
    ...valueIfWritable(fields, request, 'article_name'),
    ...valueIfWritable(fields, request, 'receiver'),
    ...valueIfWritable(fields, request, 'price_cents'),

    ...valueIfWritable(fields, request, 'requested_quantity'),
    ...valueIfWritable(fields, request, 'approved_quantity'),
    ...valueIfWritable(fields, request, 'order_quantity'),

    replacement: boolify(
      'replacement',
      valueIfWritable(fields, request, 'replacement')
    ),

    attachments: f.map(
      valueIfWritable(fields, request, 'attachments').attachments,
      o => ({ ...f.pick(o, 'id', '__typename'), to_delete: !!o.toDelete })
    ),

    // TODO: form field with id (autocomplete)
    // ...valueIfWritable(fields, request, 'supplier'),

    ...valueIfWritable(fields, request, 'article_number'),
    ...valueIfWritable(fields, request, 'motivation'),
    ...valueIfWritable(fields, request, 'priority'),
    ...valueIfWritable(fields, request, 'inspector_priority'),

    ...valueIfWritable(fields, request, 'inspection_comment'),

    ...valueIfWritable(fields, request, 'accounting_type'),
    ...valueIfWritable(fields, request, 'internal_order_number'),

    // NOTE: no building, just room!
    // ...valueIfWritable(fields, request, 'room', 'room_id'),
    ...valueIfWritable(fields, request, 'room', 'room'),

    // onyl for create:
    budget_period: preselection.budgetPeriod,
    category: preselection.category,
    // template: preselection.template,

    // FIXME: hacky hardcode
    organization: '37bbe3df-7553-5d5f-82fa-7e9a2d94951a',
    user: '7da6733c-c819-5613-8cad-2a40f51c90da'
  }

  mutate({ variables: { requestData } })
}

const readFromQueryParams = params => ({
  budget_period: f.enhyphenUUID(params.bp),
  category: f.enhyphenUUID(params.category),
  template: f.enhyphenUUID(params.template)
})

const updateQueryParams = ({ fields, params, location }) => {
  const formParams = {
    bp: f.dehyphenUUID(fields.budget_period),
    category: f.dehyphenUUID(fields.category),
    template: f.dehyphenUUID(fields.template)
  }
  return {
    ...location,
    search: '?' + qs.stringify({ ...params, ...formParams })
  }
}

const RequestNewPage = () => (
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
                key={location.key} // reset state on location change!
                data={data}
                initialSelection={readFromQueryParams(params)}
                onSelectionChange={fields =>
                  history.replace(
                    updateQueryParams({ params, location, fields })
                  )
                }
              />
            )
          }}
        </Query>
      </MainWithSidebar>
    )}
  </RouteParams>
)

export default RequestNewPage

class NewRequestPreselection extends React.Component {
  constructor(props) {
    super(props)
    // apply initial selection from URL parameters
    this.state = { initalSelection: props.initialSelection }
  }
  render({ state, props: { data, onSelectionChange } } = this) {
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

    // FIXME: is not applied on first load???
    const defaultSelection = { budget_period: budPeriods[0].value }

    return (
      <StatefulForm
        idPrefix={`request_new`}
        values={{ ...defaultSelection, ...state.initalSelection }}
        onChange={fields => onSelectionChange(fields)}
      >
        {({ fields, setValue, getValue, formPropsFor, ...formHelpers }) => {
          const resetSelection = () => {
            setValue('template', undefined)
            setValue('category', undefined)
          }
          const selectedCategory = fields.category
          const selectedTemplate = f.find(data.templates, {
            id: fields.template
          })
          const hasPreselected = !!(selectedTemplate || selectedCategory)

          return (
            <F>
              <form
                onSubmit={e => {
                  e.preventDefault()
                  window.alert('TODO!')
                }}
              >
                <FormGroup
                  label="Budgetperiode"
                  className="form-control-lg p-0"
                >
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
                  {/* TODO: clear selection buttons on both boxes (like request.form.cancel) */}
                  {hasPreselected &&
                    (selectedCategory ? (
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
                    ))}

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
                  budgetPeriod={fields.budget_period}
                  category={fields.category}
                  template={fields.template}
                  onCancel={resetSelection}
                  // NOTE: reset form if preselection changes
                  // TODO: remove this, should not be needed anymore
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
}

const NewRequestForm = ({ budgetPeriod, template, category, onCancel }) => (
  <F>
    <Query
      query={NEW_REQUEST_QUERY}
      variables={{ budgetPeriod, template, category }}
      networkPolicy="network-only"
    >
      {({ loading, error, data }) => {
        if (loading) return <Loading />
        if (error) return <ErrorPanel error={error} data={data} />

        const request = {
          ...data.new_request,
          id: `new_${String(Math.random()).slice(2, 12)}`
        }

        return (
          <Mutation mutation={CREATE_REQUEST_MUTATION}>
            {(mutate, mutReq) => {
              if (mutReq.loading) return <Loading />
              if (mutReq.error)
                return <ErrorPanel error={mutReq.error} data={mutReq.data} />

              if (mutReq.called) {
                return (
                  <div>
                    <pre>
                      <mark>OK!</mark>
                    </pre>

                    <RequestLineClosed request={data.request} />
                  </div>
                )
              }

              return (
                <F>
                  <hr className="my-4" />
                  <RequestForm
                    request={request}
                    categories={data.main_categories}
                    budgetPeriods={data.budget_periods}
                    onCancel={onCancel}
                    onSubmit={fields =>
                      updateRequestFromFields(mutate, request, fields, {
                        budgetPeriod,
                        template,
                        category
                      })
                    }
                  />
                </F>
              )
            }}
          </Mutation>
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
