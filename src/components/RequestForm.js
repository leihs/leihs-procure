import React from 'react'
import cx from 'classnames'
import f from 'lodash'

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
  ControlledForm
} from './Bootstrap'

import { RequestTotalAmount as TotalAmount } from './decorators'
import BuildingAutocomplete from './BuildingAutocomplete'
import RoomAutocomplete from './RoomAutocomplete'

const prepareFormValues = request => {
  const fields = f.mapValues(f.omit(request, ['room', 'building']), field => {
    if (f.isObject(field)) {
      return field.value
    }
    return field
  })
  fields.room = f.get(request, 'room.value.id')
  fields.building = f.get(request, 'room.value.building.id')
  return fields
}

const RequestForm = ({ request, className, onClose, onSubmit }) => {
  const fields = prepareFormValues(request)

  return (
    <ControlledForm idPrefix={`request_form_${request.id}`} values={fields}>
      {({ fields, ...formHelper }) => {
        // add translated labels:
        const formPropsFor = k => ({
          ...formHelper.formPropsFor(k),
          label: t(`request_form_field.${k}`)
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

                <FormField {...formPropsFor('supplier')} />

                <FormField {...formPropsFor('receiver')} />

                <FormGroup label={formPropsFor('building').label}>
                  <BuildingAutocomplete {...formPropsFor('building')} />
                </FormGroup>

                <FormGroup label={formPropsFor('room').label}>
                  <RoomAutocomplete
                    {...formPropsFor('room')}
                    buildingId={fields.building}
                  />
                </FormGroup>

                {request.motivation.write ? (
                  <FormField type="textarea" {...formPropsFor('motivation')} />
                ) : (
                  fields.motivation
                )}

                <Row>
                  <Col sm>
                    <FormGroup>
                      <Select
                        {...formPropsFor('priority_requester')}
                        options={[0, 1, 2, 3].map(n => ({
                          value: n,
                          label: t(
                            `request_form_field.request_priority_inspector_labels.${n}`
                          )
                        }))}
                      />
                    </FormGroup>
                  </Col>
                  <Col sm>
                    <FormGroup>
                      <Select
                        {...formPropsFor('priority_inspector')}
                        options={[0, 1, 2, 3].map(n => ({
                          value: n,
                          label: t(
                            `request_form_field.request_priority_inspector_labels.${n}`
                          )
                        }))}
                      />
                    </FormGroup>
                  </Col>
                </Row>

                <FormGroup>
                  <ButtonRadio
                    {...formPropsFor('replacement')}
                    options={['replacement', 'new'].map(k => ({
                      value: k,
                      label: t(
                        `request_form_field.request_replacement_labels_${k}`
                      )
                    }))}
                  />
                </FormGroup>
              </Col>

              <Col lg>
                <Row>
                  <Col sm="8">
                    <FormField
                      {...formPropsFor('price')}
                      type="number-integer"
                      labelSmall={t('request_form_field.price_help')}
                      helpText="Bitte nur ganze Zahlen eingeben"
                    />
                  </Col>
                  <Col sm="4">
                    <FormField
                      type="text-static"
                      name="price_total"
                      value={TotalAmount(fields)}
                      label={t('request_form_field.price_total')}
                      labelSmall={t('request_form_field.price_help')}
                    />
                  </Col>
                </Row>

                <Row>
                  <Col sm>
                    <FormField
                      {...formPropsFor('quantity_requested')}
                      type="number-integer"
                    />
                  </Col>
                  <Col sm>
                    <FormField
                      {...formPropsFor('quantity_approved')}
                      type="number-integer"
                      max={fields.quantity_requested}
                    />
                  </Col>
                  <Col sm>
                    <FormField
                      {...formPropsFor('quantity_ordered')}
                      type="number-integer"
                      max={fields.quantity_approved}
                    />
                  </Col>
                </Row>

                <FormField
                  {...formPropsFor('inspection_comment')}
                  type="textarea"
                  beforeInput={
                    <Select
                      id="priority_requester"
                      m="b-3"
                      cls="form-control-sm"
                      options={['foo', 'bar', 'baz'].map(s => ({
                        value: s,
                        label: s
                      }))}
                    />
                  }
                />

                <FormGroup label={t('request_form_field.attachments')}>
                  <FilePicker id="attachments" name="attachments" />
                </FormGroup>

                {/* FIXME: accounting_type
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
                  <FormField {...formPropsFor('cost_center')} />
                ) : (
                  <Row>
                    <Col sm>
                      <FormField {...formPropsFor('internal_order_number')} />
                    </Col>

                    <Col sm>
                      <FormField
                        type="text-static"
                        value="123456789"
                        name="general_ledger_account"
                      />
                    </Col>
                  </Row>
                )} */}
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
                  <Icon.Calendar /> {t('form_btn_change_budget_period')}
                </button>
                <button
                  type="button"
                  className="btn m-1 btn-outline-danger btn-massive"
                  onClick={() => window.alert('TODO!')}
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
                <button
                  type="button"
                  className="btn m-1 btn-outline-secondary btn-massive"
                  onClick={onClose}
                >
                  {t('form_btn_cancel')}
                </button>
              </Col>
            </Row>
            {/* <pre>{JSON.stringify({ fields }, 0, 2)}</pre> */}
          </form>
        )
      }}
    </ControlledForm>
  )
}

export default RequestForm
