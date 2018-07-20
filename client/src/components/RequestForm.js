import React from 'react'
import cx from 'classnames'
import f from 'lodash'

import * as CONSTANTS from '../constants'
import t from '../locale/translate'
import Icon from './Icons'
import {
  Row,
  Col,
  FilePicker,
  FormGroup,
  FormField,
  Select,
  ButtonRadio,
  StatefulForm
} from './Bootstrap'

import { RequestTotalAmount as TotalAmount, formatCurrency } from './decorators'
import BuildingAutocomplete from './BuildingAutocomplete'
import RoomAutocomplete from './RoomAutocomplete'

const tmpUppercasey = v => (f.isString(v) ? v.toUpperCase() : v)

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
  return fields
}

const RequestForm = ({ request, className, onClose, onSubmit, ...props }) => {
  return (
    <StatefulForm
      idPrefix={`request_form_${request.id}`}
      values={prepareFormValues(request)}
    >
      {({ fields, setValue, getValue, ...formHelpers }) => {
        const formPropsFor = key => ({
          ...formHelpers.formPropsFor(key),
          // add translated labels:
          label: t(`request_form_field.${key}`),
          // add readonly by field permissions
          readOnly: request[key] ? !request[key].write : null
        })

        return (
          <form
            id={request.id}
            className={cx(className)}
            onSubmit={e => {
              e.preventDefault()
              onSubmit(fields)
            }}
          >
            <Row>
              <Col lg>
                <FormField {...formPropsFor('article_name')} />

                <FormField {...formPropsFor('article_number')} />

                {/* FIXME: handle field ID vs String */}
                <FormField {...formPropsFor('supplier')} />
                <FormField {...formPropsFor('supplier_name')} />

                <FormField {...formPropsFor('receiver')} />

                <FormGroup label={formPropsFor('building').label}>
                  <BuildingAutocomplete
                    {...formPropsFor('building')}
                    disabled={formPropsFor('room').readOnly}
                    readOnly={formPropsFor('room').readOnly}
                  />
                </FormGroup>

                <FormGroup label={formPropsFor('room').label}>
                  <RoomAutocomplete
                    {...formPropsFor('room')}
                    disabled={formPropsFor('room').readOnly}
                    buildingId={fields.building}
                  />
                </FormGroup>

                <FormField
                  type="textarea"
                  minRows="5"
                  {...formPropsFor('motivation')}
                />

                <Row>
                  <Col sm>
                    <FormGroup label={t('request_form_field.priority')}>
                      <Select
                        {...formPropsFor('priority')}
                        options={CONSTANTS.REQUEST_PRIORITIES.map(v => ({
                          value: v,
                          label: t(`priority_label_${v}`)
                        }))}
                      />
                    </FormGroup>
                  </Col>
                  <Col sm>
                    <FormGroup
                      label={t('request_form_field.inspector_priority')}
                    >
                      <Select
                        {...formPropsFor('inspector_priority')}
                        options={CONSTANTS.REQUEST_INSPECTOR_PRIORITIES.map(
                          v => ({
                            value: v,
                            label: t(`inspector_priority_label_${v}`)
                          })
                        )}
                      />
                    </FormGroup>
                  </Col>
                </Row>

                {/* TODO: replacement with BOOLs or ENUMs
                  <FormGroup>
                  <ButtonRadio
                    {...formPropsFor('replacement')}
                    options={CONSTANTS.REQUEST_REPLACEMENT_VALUES.map(v => ({
                      value: v,
                      label: t(
                        `request_form_field.request_replacement_labels_${v}`
                      )
                    }))}
                  />
                </FormGroup> */}
              </Col>

              <Col lg>
                <Row>
                  <Col sm="8">
                    <FormField
                      {...formPropsFor('price_cents')}
                      value={(getValue('price_cents') || 0) / 100}
                      onChange={e =>
                        setValue('price_cents', e.target.value * 100)
                      }
                      type="number-integer"
                      labelSmall={t('request_form_field.price_help')}
                      helpText="Bitte nur ganze Zahlen eingeben"
                    />
                  </Col>

                  <Col sm="4">
                    <FormField
                      type="text-static"
                      name="price_total"
                      value={formatCurrency(TotalAmount(fields))}
                      label={t('request_form_field.price_total')}
                      labelSmall={t('request_form_field.price_help')}
                    />
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
                        // NOTE: we dont want to keep the selected value and ust use it once.
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
                  <FilePicker id="attachments" name="attachments" />
                </FormGroup>

                <FormGroup>
                  <ButtonRadio
                    {...formPropsFor('accounting_type')}
                    options={['aquisition', 'investment'].map(k => ({
                      value: k,
                      label: t(`request_form_field.accounting_type_label_${k}`)
                    }))}
                  />
                </FormGroup>

                {fields.accounting_type !== 'investment' ? (
                  <Row>
                    <Col>
                      <FormField readOnly {...formPropsFor('cost_center')} />
                    </Col>
                    <Col>
                      <FormField
                        readOnly
                        {...formPropsFor('procurement_account')}
                      />
                    </Col>
                  </Row>
                ) : (
                  <Row>
                    <Col sm>
                      <FormField {...formPropsFor('internal_order_number')} />
                    </Col>

                    <Col sm>
                      <FormField
                        readOnly
                        value="123456789"
                        name="general_ledger_account"
                        label="general_ledger_account"
                      />
                    </Col>
                  </Row>
                )}
              </Col>
            </Row>

            <hr m="mt-0" />

            <Row m="t-5">
              <Col lg>
                <button
                  type="button"
                  className="btn m-1 btn-outline-dark btn-massive"
                  onClick={() => window.alert('TODO!')}
                >
                  <Icon.Exchange /> {t('form_btn_move_category')}
                </button>
                <button
                  type="button"
                  className="btn m-1 btn-outline-dark btn-massive"
                  onClick={() => window.alert('TODO!')}
                >
                  <Icon.BudgetPeriod /> {t('form_btn_change_budget_period')}
                </button>
                <button
                  type="button"
                  className="btn m-1 btn-outline-danger btn-massive"
                  onClick={props.doDeleteRequest}
                >
                  <Icon.Trash /> {t('form_btn_delete')}
                </button>
              </Col>
              <Col lg order="first" className="mt-3 mt-lg-0">
                <button
                  type="submit"
                  className="btn m-1 btn-primary btn-massive"
                >
                  <Icon.Checkmark /> <span>{t('form_btn_save')}</span>
                </button>
                {!!onClose && (
                  <button
                    type="button"
                    className="btn m-1 btn-outline-secondary btn-massive"
                    onClick={onClose}
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

export default RequestForm
