import React, { Fragment as F } from 'react'
import f from 'lodash'
import cx from 'classnames'
import { Query } from 'react-apollo'
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

        // const data = FAKE_DATA[template ? 'FROM_TEMPLATE' : 'FROM_CATEGORY']

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
