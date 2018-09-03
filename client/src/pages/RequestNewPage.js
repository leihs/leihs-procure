import React, { Fragment as F } from 'react'
import f from 'lodash'
import cx from 'classnames'
import { Query, Mutation } from 'react-apollo'
import gql from 'graphql-tag'
import qs from 'qs'

// import * as CONSTANTS from '../constants'
import * as Fragments from '../graphql-fragments'
// import t from '../locale/translate'
import { Redirect } from '../components/Router'
import Icon from '../components/Icons'
import {
  Row,
  Col,
  Button,
  Badge,
  Collapsing,
  FormGroup,
  Select,
  StatefulForm,
  RouteParams
} from '../components/Bootstrap'
import { MainWithSidebar } from '../components/Layout'
import Loading from '../components/Loading'
import { ErrorPanel } from '../components/Error'
import { DisplayName, formatCurrency } from '../components/decorators'
import ImageThumbnail from '../components/ImageThumbnail'

import RequestForm from '../components/RequestForm'
import { requestDataFromFields as requestDataFromFieldsBase } from '../containers/RequestEdit'

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
        version
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
  mutation createRequest($requestData: CreateRequestInput) {
    create_request(input_data: $requestData) {
      # NOTE: only redirect for now
      id
      # ...RequestFieldsForEdit
    }
  }
  #{Fragments.RequestFieldsForEdit}
`

const requestDataFromFields = (request, fields, preselection) => ({
  ...requestDataFromFieldsBase(request, fields),
  // only for create:
  budget_period: preselection.budgetPeriod,
  category: preselection.category,
  template: preselection.template
})

const readFromQueryParams = params => ({
  budgetPeriod: f.enhyphenUUID(params.bp),
  mainCategory: f.enhyphenUUID(params.mc),
  category: f.enhyphenUUID(params.c),
  template: f.enhyphenUUID(params.t)
})

const updateQueryParams = ({ fields, params, location }) => {
  const formParams = {
    bp: f.dehyphenUUID(fields.budgetPeriod),
    mc: f.dehyphenUUID(fields.mainCategory),
    c: f.dehyphenUUID(fields.category),
    t: f.dehyphenUUID(fields.template)
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
          fetchPolicy="cache-then-network"
        >
          {({ loading, error, data }) => {
            if (loading) return <Loading />
            if (error) return <ErrorPanel error={error} data={data} />
            const budgetPeriods = data.budget_periods.filter(
              bp => new Date(bp.inspection_start_date).getTime() > Date.now()
            )

            return (
              <NewRequestPreselection
                key={location.key} // reset state on location change!
                data={data}
                budgetPeriods={budgetPeriods}
                selection={readFromQueryParams(params)}
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
  render(
    {
      props: { data, budgetPeriods, selection, onSelectionChange, formKey }
    } = this
  ) {
    const budPeriods = budgetPeriods.map(bp => ({
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
        values={selection}
        key={JSON.stringify(selection)}
      >
        {({ fields, setValue, setValues, ...formHelpers }) => {
          const selectedBudgetPeriod = fields.budgetPeriod
          const selectedCategory = fields.category
          const selectedMainCat = f.find(data.main_categories, {
            id: fields.mainCategory
          })
          const selectedTemplate = f.find(data.templates, {
            id: fields.template
          })
          const hasPreselected = !!(selectedTemplate || selectedCategory)
          const hasPreselectedAll = !!(selectedBudgetPeriod && hasPreselected)

          const formPropsFor = name => ({
            ...formHelpers.formPropsFor(name),
            onChange: e => setSelection({ [name]: e.target.value })
          })
          const setSelection = ({ mainCategory, category, template } = {}) => {
            onSelectionChange({
              ...f.omit(fields, ['mainCategory', 'category', 'template']),
              mainCategory,
              category,
              template
            })
          }
          const resetTemplate = () => {
            if (!selectedTemplate) return setSelection()
            setSelection({ category: selectedTemplate.category.id })
          }

          // NOTE: if a MC is selected in params, show only it
          const shownMainCats = f.filter(
            data.main_categories,
            !selectedMainCat ? {} : { id: selectedMainCat.id }
          )

          const categoryTree = shownMainCats.map(mc => ({
            ...mc,
            categories: mc.categories.map(sc => ({
              ...sc,
              templates: f.filter(data.templates, { category: { id: sc.id } })
            }))
          }))

          // only show validations in error case (only red no green)
          const showValidations = !selectedBudgetPeriod

          return (
            <F>
              <form className={cx({ 'was-validated': showValidations })}>
                <FormGroup label="Budgetperiode" className="form-group-lg">
                  <Select
                    {...formPropsFor('budgetPeriod')}
                    className="custom-select-lg"
                    required
                    options={budPeriods}
                  />
                </FormGroup>

                <FormGroup
                  label="Kategorie & Vorlage"
                  className="form-group-lg"
                >
                  <Row cls="mb-3">
                    <Let
                      tpl={selectedTemplate}
                      mc={selectedMainCat}
                      cat={
                        selectedCategory
                          ? CatWithMainCat(selectedCategory)
                          : selectedTemplate
                            ? CatWithMainCat(selectedTemplate.category.id)
                            : null
                      }
                    >
                      {({ mc, cat, tpl }) => (
                        <F>
                          <Col sm>
                            {!!(mc || cat) && (
                              <SelectionCard
                                onRemoveClick={() => setSelection()}
                              >
                                <Icon.Categories spaced="2" />
                                {mc ? (
                                  mc.name
                                ) : (
                                  <F>
                                    {cat.main_category.name}
                                    <Icon.CaretRight />
                                    {cat.name}
                                  </F>
                                )}
                              </SelectionCard>
                            )}
                          </Col>

                          <Col sm>
                            {hasPreselected &&
                              (!tpl ? (
                                <SelectionCard>Keine Vorlage</SelectionCard>
                              ) : (
                                <SelectionCard onRemoveClick={resetTemplate}>
                                  <Icon.Templates spaced />
                                  {tpl.article_name || DisplayName(tpl.model)}
                                </SelectionCard>
                              ))}
                          </Col>
                        </F>
                      )}
                    </Let>
                  </Row>

                  {!hasPreselected && (
                    <Let
                      onSelectCategory={c => setSelection({ category: c.id })}
                      onSelectTemplate={t => setSelection({ template: t.id })}
                    >
                      {({ onSelectCategory, onSelectTemplate }) =>
                        selectedMainCat ? (
                          <div className="card">
                            <CategoryItemsList
                              items={categoryTree[0].categories}
                              onSelectCategory={onSelectCategory}
                              onSelectTemplate={onSelectTemplate}
                            />
                          </div>
                        ) : (
                          <CategoriesTemplatesTree
                            mainCategories={categoryTree}
                            onSelectCategory={onSelectCategory}
                            onSelectTemplate={onSelectTemplate}
                            onSelectMaincat={m =>
                              setSelection({ mainCategory: m.id })
                            }
                          />
                        )
                      }
                    </Let>
                  )}
                </FormGroup>
              </form>
              {window.isDebug && <pre>{JSON.stringify(fields, 0, 2)}</pre>}

              {hasPreselectedAll && (
                <NewRequestForm
                  budgetPeriod={fields.budgetPeriod}
                  category={fields.category}
                  template={fields.template}
                  onCancel={() => setSelection()}
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
      fetchPolicy="network-only"
    >
      {({ loading, error, data }) => {
        if (loading) return <Loading />
        if (error) return <ErrorPanel error={error} data={data} />

        const request = data.new_request

        return (
          <Mutation mutation={CREATE_REQUEST_MUTATION}>
            {(mutate, mutReq) => {
              if (mutReq.loading) return <Loading />
              if (mutReq.error)
                return <ErrorPanel error={mutReq.error} data={mutReq.data} />

              if (mutReq.called) {
                return (
                  <Redirect
                    push // dont replace current route!
                    scrollTop
                    to={{
                      pathname: `/requests/${mutReq.data.create_request.id}`,
                      state: {
                        flash: {
                          level: 'success',
                          message: 'Antrag wurde erfolgreich erstellt'
                        }
                      }
                    }}
                  />
                )
              }

              return (
                <F>
                  <hr className="my-4" />
                  <RequestForm
                    id="new_request"
                    request={request}
                    categories={data.main_categories}
                    budgetPeriods={data.budgetPeriods}
                    onCancel={onCancel}
                    onSubmit={fields =>
                      mutate({
                        variables: {
                          requestData: requestDataFromFields(request, fields, {
                            budgetPeriod,
                            template,
                            category
                          })
                        }
                      })
                    }
                  />
                  {window.isDebug && (
                    <pre>{JSON.stringify({ request }, 0, 2)}</pre>
                  )}
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
  mainCategories,
  onSelectCategory,
  onSelectMaincat,
  onSelectTemplate
}) => {
  return (
    <F>
      <ul className="list-unstyled">
        {mainCategories.map(mc => (
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
                    <CategoryItemsList
                      items={mc.categories}
                      onSelectCategory={onSelectCategory}
                      onSelectTemplate={onSelectTemplate}
                    />
                  )}
                </li>
              )}
            </Collapsing>
          </F>
        ))}
      </ul>
    </F>
  )
}

const CategoryItemsList = ({ items, onSelectCategory, onSelectTemplate }) => {
  return (
    <ul className="list-group list-group-flush">
      {items.map(sc => (
        <F key={sc.id}>
          <li className="card list-group-item p-0">
            {!f.any(sc.templates, l => !f.isEmpty(l)) ? (
              <AddButtonLine onClick={e => onSelectCategory(sc)}>
                {sc.name}
              </AddButtonLine>
            ) : (
              <F>
                <h4 className="card-header h5">{sc.name}</h4>

                <div className="list-group list-group-flush">
                  {sc.templates.map(t => (
                    <AddButtonLine
                      key={t.id}
                      t={t}
                      onClick={e => {
                        e.preventDefault()
                        onSelectTemplate(t)
                      }}
                    >
                      <Icon.Templates /> {t.article_name}{' '}
                      <Badge secondary>
                        <samp>{formatCurrency(t.price_cents)}</samp>
                      </Badge>
                    </AddButtonLine>
                  ))}

                  <AddButtonLine onClick={e => onSelectCategory(sc)}>
                    {'Ohne Vorlage erstellen'}
                  </AddButtonLine>
                </div>
              </F>
            )}
          </li>
        </F>
      ))}
    </ul>
  )
}

const SelectionCard = ({ children, onRemoveClick }) => (
  <div className="card">
    <div className="card-body px-3 py-2 d-flex justify-content-between align-items-center">
      <div>{children}</div>
      {!!onRemoveClick && (
        <Button color="link" outline size="sm" onClick={e => onRemoveClick()}>
          <Icon.Cross />
        </Button>
      )}
    </div>
  </div>
)

const AddButtonLine = ({ children, ...props }) => (
  <button
    className="list-group-item list-group-item-action cursor-pointer"
    {...props}
  >
    <Icon.PlusCircle color="success" className="mr-3" size="lg" />
    {children}
  </button>
)

const Let = ({ children, ...props }) => children(props)
