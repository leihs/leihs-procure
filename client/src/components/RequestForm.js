import React, { Fragment as F } from 'react'
// import PropTypes from 'prop-types'
import cx from 'classnames'
import f from 'lodash'

import * as CONSTANTS from '../constants'
import t from '../locale/translate'
import Icon from './Icons'
import {
  Row,
  Col,
  InputFileUpload,
  FormGroup,
  FormField,
  Select,
  ButtonRadio,
  StatefulForm,
  ButtonDropdown,
  DropdownToggle,
  DropdownMenu,
  DropdownItem
} from './Bootstrap'

import { RequestTotalAmount as TotalAmount, formatCurrency } from './decorators'
import * as formBlocker from './FormBlocker'
import { ConfirmFormNav } from './FormBlocker'
import BuildingAutocomplete from './BuildingAutocomplete'
import RoomAutocomplete from './RoomAutocomplete'
import ModelAutocomplete from './ModelAutocomplete'
import SupplierAutocomplete from './SupplierAutocomplete'
import UserAutocomplete from './UserAutocomplete'

const tmpUppercasey = v => (f.isString(v) ? v.toUpperCase() : v)

const nbsp = '\u00A0'

const prepareFormValues = request => {
  const fields = f.mapValues(f.omit(request, ['room', 'building']), field => {
    if (f.isObject(field)) {
      return field.value
    }
    return field
  })
  // FIXME: server response has lowercase???
  fields.priority = tmpUppercasey(f.get(request, 'priority.value'))
  fields.inspector_priority = tmpUppercasey(
    f.get(request, 'inspector_priority.value')
  )

  fields.room = f.get(request, 'room.value.id')
  fields.building = f.get(request, 'room.value.building.id')

  // extra form controlls
  fields._model_as_text = f.present(fields.article_name)
  fields._supplier_as_text = f.present(fields.supplier_name)
  return fields
}

const requiredLabel = (label, required) => label + (required ? `${nbsp}*` : '')

class RequestForm extends React.Component {
  state = { showValidations: false, ...formBlocker.initialState }
  showValidations = (bool = true) => this.setState({ showValidations: bool })
  isDisabled = (p = this.props) => p.disabled || p.readOnly
  render(
    {
      state,
      props: { request, id, className, onCancel, onSubmit, ...props }
    } = this
  ) {
    const formId = id || request.id
    if (!formId) throw new Error('missing ID!')

    return (
      <StatefulForm
        idPrefix={`request_form_${formId}`}
        values={prepareFormValues(request)}
      >
        {({ fields, setValue, getValue, ...formHelpers }) => {
          const formPropsFor = key => {
            const required = request[key] ? request[key].required : null
            const readOnly = request[key] ? !request[key].write : null
            return {
              ...formHelpers.formPropsFor(key),
              // add translated labels:
              label: requiredLabel(t(`request_form_field.${key}`), required),
              hidden: request[key] ? !request[key].read : false,
              required,
              readOnly
            }
          }

          return (
            <form
              id={formId}
              className={cx(className, {
                'was-validated': this.state.showValidations
              })}
              onSubmit={e => {
                e.preventDefault()
                onSubmit(fields, () => formBlocker.unblock(this))
              }}
              // NOTE: dont calculate real form diff, just set blocking on first change
              onChange={f.once(e => formBlocker.block(this))}
            >
              <ConfirmFormNav when={state.isBlocking} />

              <Row>
                <Col lg>
                  <Let
                    articleField={formPropsFor('article_name')}
                    modelField={formPropsFor('model')}
                  >
                    {({ articleField, modelField }) => {
                      // NOTE: dependent field model OR article_name
                      const anyRequired =
                        articleField.required || modelField.required
                      return (
                        <Row cls="no-gutters">
                          <Col sm>
                            {fields._model_as_text ? (
                              <FormField {...articleField} />
                            ) : (
                              <FormGroup label={articleField.label}>
                                <ModelAutocomplete
                                  {...modelField}
                                  label={articleField.label}
                                  required={anyRequired}
                                />
                              </FormGroup>
                            )}
                          </Col>
                          <Col sm="3" cls="pl-sm-3">
                            <FieldTypeToggle
                              {...formPropsFor('_model_as_text')}
                              checked={!!formPropsFor('_model_as_text').value}
                              disabled={articleField.readOnly}
                              label={t('form_input_check_free_text')}
                            />
                          </Col>
                        </Row>
                      )
                    }}
                  </Let>

                  <FormField {...formPropsFor('article_number')} />

                  <RequestInput field={formPropsFor('supplier')}>
                    {supplierField => (
                      <Row cls="no-gutters">
                        <Col sm>
                          {fields._supplier_as_text ? (
                            <FormField
                              {...formPropsFor('supplier_name')}
                              readOnly={supplierField.readOnly}
                            />
                          ) : (
                            <FormGroup label={supplierField.label}>
                              <SupplierAutocomplete {...supplierField} />
                            </FormGroup>
                          )}
                        </Col>
                        <Col sm="3" cls="pl-sm-3">
                          <FieldTypeToggle
                            {...formPropsFor('_supplier_as_text')}
                            disabled={supplierField.readOnly}
                            label={t('form_input_check_free_text')}
                          />
                        </Col>
                      </Row>
                    )}
                  </RequestInput>

                  <FormField {...formPropsFor('receiver')} />

                  <RequestInput field={formPropsFor('room')}>
                    {roomField => (
                      <F>
                        <FormGroup
                          label={requiredLabel(
                            formPropsFor('building').label,
                            roomField.required
                          )}
                        >
                          <BuildingAutocomplete
                            {...formPropsFor('building')}
                            disabled={roomField.readOnly}
                            readOnly={roomField.readOnly}
                            required={roomField.required}
                          />
                        </FormGroup>

                        <FormGroup label={roomField.label}>
                          <RoomAutocomplete
                            {...roomField}
                            disabled={roomField.readOnly}
                            buildingId={fields.building}
                          />
                        </FormGroup>
                      </F>
                    )}
                  </RequestInput>

                  <FormField
                    type="textarea"
                    minRows="5"
                    {...formPropsFor('motivation')}
                  />

                  <Row>
                    <RequestInput field={formPropsFor('priority')}>
                      {field => (
                        <Col sm>
                          <FormGroup label={field.label}>
                            <Select
                              {...field}
                              options={CONSTANTS.REQUEST_PRIORITIES.map(v => ({
                                value: v,
                                label: t(`priority_label_${v}`)
                              }))}
                            />
                          </FormGroup>
                        </Col>
                      )}
                    </RequestInput>

                    <RequestInput field={formPropsFor('inspector_priority')}>
                      {field => (
                        <Col sm>
                          <FormGroup label={field.label}>
                            <Select
                              {...field}
                              options={CONSTANTS.REQUEST_INSPECTOR_PRIORITIES.map(
                                v => ({
                                  value: v,
                                  label: t(`inspector_priority_label_${v}`)
                                })
                              )}
                            />
                          </FormGroup>
                        </Col>
                      )}
                    </RequestInput>
                  </Row>

                  <RequestInput field={formPropsFor('replacement')}>
                    {field => (
                      <FormGroup label={field.label}>
                        <ButtonRadio
                          {...formPropsFor('replacement')}
                          value={
                            f.isBoolean(field.value)
                              ? CONSTANTS.REQUEST_REPLACEMENT_VALUES_MAP[
                                  field.value
                                ]
                              : field.value
                          }
                          options={CONSTANTS.REQUEST_REPLACEMENT_VALUES.map(
                            v => ({
                              value: v,
                              label: t(
                                `request_form_field.request_replacement_labels_${v}`
                              )
                            })
                          )}
                        />
                      </FormGroup>
                    )}
                  </RequestInput>
                </Col>

                <Col lg>
                  <Row>
                    <Col sm="8">
                      <RequestInput field={formPropsFor('price_cents')}>
                        {priceField => (
                          <FormField
                            type="number-integer"
                            {...priceField}
                            labelSmall={t('request_form_field.price_help')}
                            helpText="Bitte nur ganze Zahlen eingeben"
                            value={
                              f.isNumber(priceField.value)
                                ? priceField.value / 100
                                : priceField.value
                            }
                            onChange={e => {
                              const val = parseInt(e.target.value, 10)
                              const num = parseInt(val, 10)
                              setValue(
                                'price_cents',
                                f.isNumber(num) && !f.isNaN(num)
                                  ? num * 100
                                  : val
                              )
                            }}
                          />
                        )}
                      </RequestInput>
                    </Col>

                    <Col sm="4">
                      <FormGroup
                        name="price_total"
                        label={t('request_form_field.price_total')}
                        labelSmall={t('request_form_field.price_help')}
                      >
                        <samp>{formatCurrency(TotalAmount(fields))}</samp>
                      </FormGroup>
                    </Col>
                  </Row>

                  <Row>
                    <Col sm>
                      <FormField
                        {...formPropsFor('requested_quantity')}
                        type="number-integer"
                      />
                    </Col>
                    <Col sm>
                      <FormField
                        {...formPropsFor('approved_quantity')}
                        type="number-integer"
                        max={fields.requested_quantity}
                      />
                    </Col>
                    <Col sm>
                      <FormField
                        {...formPropsFor('order_quantity')}
                        type="number-integer"
                        max={fields.approved_quantity}
                      />
                    </Col>
                  </Row>

                  <FormField
                    {...formPropsFor('inspection_comment')}
                    type="textarea"
                    beforeInput={
                      !formPropsFor('inspection_comment').readOnly && (
                        <Select
                          id="priority_requester"
                          m="b-3"
                          cls="form-control-sm"
                          options={['foo', 'bar', 'baz'].map(s => ({
                            value: s,
                            label: s
                          }))}
                          disabled={formPropsFor('inspection_comment').readOnly}
                          // NOTE: we dont want to keep the selected value and just use it once.
                          // Always setting empty value makes it controlled and React resets it for us!
                          value={''}
                          onChange={({ target: { value } }) => {
                            setValue(
                              'inspection_comment',
                              value + '\n' + getValue('inspection_comment')
                            )
                          }}
                        />
                      )
                    }
                  />

                  <FormGroup label={t('request_form_field.attachments')}>
                    <InputFileUpload {...formPropsFor('attachments')} />
                  </FormGroup>

                  <RequestInput field={formPropsFor('user')}>
                    {userField =>
                      // NOTE: don't show at all if not writable
                      !userField.readOnly && (
                        <FormGroup label={userField.label}>
                          <UserAutocomplete onlyRequesters {...userField} />
                        </FormGroup>
                      )
                    }
                  </RequestInput>

                  <Let accTypeField={formPropsFor('accounting_type')}>
                    {({ accTypeField }) =>
                      // NOTE: don't show at all if not writable
                      !accTypeField.readOnly && (
                        <F>
                          <FormGroup label={accTypeField.label}>
                            <ButtonRadio
                              {...accTypeField}
                              options={['aquisition', 'investment'].map(k => ({
                                value: k,
                                label: t(
                                  `request_form_field.accounting_type_label_${k}`
                                )
                              }))}
                            />
                          </FormGroup>

                          {!!accTypeField.value &&
                            (accTypeField.value !== 'investment' ? (
                              <Row>
                                <Col>
                                  <FormField
                                    {...formPropsFor('cost_center')}
                                    readOnly
                                  />
                                </Col>
                                <Col>
                                  <FormField
                                    {...formPropsFor('procurement_account')}
                                    readOnly
                                  />
                                </Col>
                              </Row>
                            ) : (
                              <Row>
                                <Col sm>
                                  <FormField
                                    {...formPropsFor('internal_order_number')}
                                    // NOTE: dependent field, always required *if* shown!
                                    required={true}
                                    label={requiredLabel(
                                      formPropsFor('internal_order_number')
                                        .label,
                                      true
                                    )}
                                  />
                                </Col>

                                <Col sm>
                                  <FormField
                                    {...formPropsFor('general_ledger_account')}
                                    readOnly
                                  />
                                </Col>
                              </Row>
                            ))}
                        </F>
                      )
                    }
                  </Let>
                </Col>
              </Row>

              <hr m="mt-0" />

              <Row m="t-5">
                <Col lg>
                  {!!props.doChangeRequestCategory && (
                    <SelectionDropdown
                      toggle={props.onSelectNewRequestCategory}
                      isOpen={props.isSelectingNewCategory}
                      options={props.categories.map(mc => ({
                        key: mc.id,
                        header: mc.name,
                        options: mc.categories.map(c => {
                          const isCurrent = c.id === request.category.value.id
                          return {
                            key: c.id,
                            disabled: isCurrent,
                            onClick: e => props.doChangeRequestCategory(c),
                            children: (
                              <F>
                                {c.name}
                                {isCurrent && (
                                  <F>
                                    {' '}
                                    <Icon.Checkmark cls="pb-1" />
                                  </F>
                                )}
                              </F>
                            )
                          }
                        })
                      }))}
                    >
                      <Icon.Exchange /> {t('form_btn_move_category')}
                    </SelectionDropdown>
                  )}

                  {!!props.doChangeBudgetPeriod && (
                    <SelectionDropdown
                      toggle={props.onSelectNewBudgetPeriod}
                      isOpen={props.isSelectingNewBudgetPeriod}
                      options={[
                        {
                          key: 1,
                          options: props.budgetPeriods.map(bp => {
                            const isCurrent =
                              bp.id === request.budget_period.value.id
                            return {
                              key: bp.id,
                              disabled: isCurrent,
                              onClick: e => props.doChangeBudgetPeriod(bp),
                              children: (
                                <F>
                                  {bp.name}
                                  {isCurrent && (
                                    <F>
                                      {' '}
                                      <Icon.Checkmark cls="pb-1" />
                                    </F>
                                  )}
                                </F>
                              )
                            }
                          })
                        }
                      ]}
                    >
                      <Icon.BudgetPeriod /> {t('form_btn_change_budget_period')}
                    </SelectionDropdown>
                  )}

                  {!!props.doDeleteRequest && (
                    <button
                      type="button"
                      className="btn m-1 btn-outline-danger btn-massive"
                      onClick={props.doDeleteRequest}
                    >
                      <Icon.Trash /> {t('form_btn_delete')}
                    </button>
                  )}
                </Col>

                <Col lg order="first" className="mt-3 mt-lg-0">
                  <button
                    type="submit"
                    className="btn m-1 btn-primary btn-massive"
                    onClick={e => this.showValidations()}
                  >
                    <Icon.Checkmark /> <span>{t('form_btn_save')}</span>
                  </button>
                  {!!onCancel && (
                    <button
                      type="button"
                      className="btn m-1 btn-outline-secondary btn-massive"
                      onClick={onCancel}
                    >
                      {t('form_btn_cancel')}
                    </button>
                  )}
                </Col>
              </Row>

              {window.isDebug && (
                <pre className="mt-4">{JSON.stringify({ fields }, 0, 2)}</pre>
              )}
            </form>
          )
        }}
      </StatefulForm>
    )
  }
}
export default RequestForm

// FIXME: require props.id OR props.request.id
// RequestForm.propTypes = {
//   request: PropTypes.shape({ id: PropTypes.string.isRequired }).isRequired
// }

const SelectionDropdown = ({
  toggle,
  isOpen,
  children,
  menuStyle,
  options
}) => (
  <ButtonDropdown direction="down" toggle={toggle} isOpen={isOpen}>
    <DropdownToggle caret className="btn m-1 btn-outline-dark btn-massive">
      {children}
    </DropdownToggle>
    <DropdownMenu
      style={{
        maxHeight: '15rem',
        overflow: 'hidden',
        overflowY: 'scroll'
      }}
    >
      {options.map(({ key, header, options }) => (
        <F key={key}>
          {!!header && (
            <DropdownItem header className="my-2">
              {header}
            </DropdownItem>
          )}
          {options.map(({ key, ...props }) => (
            <DropdownItem key={key} {...props} />
          ))}
        </F>
      ))}
    </DropdownMenu>
  </ButtonDropdown>
)

const FieldTypeToggle = ({ id, value, label, ...props }) => (
  <FormGroup>
    <div className="custom-control custom-checkbox mt-sm-4 pt-sm-3">
      <input
        type="checkbox"
        className="custom-control-input"
        formNoValidate
        id={id}
        {...props}
        checked={value}
        value={undefined}
        label={undefined}
      />
      <label className="custom-control-label" htmlFor={id}>
        <small>{label.replace(/\s/g, nbsp)}</small>
      </label>
    </div>
  </FormGroup>
)

const Let = ({ children, ...props }) => children(props)
const RequestInput = ({ children, field }) => (
  <F>{!field.hidden && children(field)}</F>
)
