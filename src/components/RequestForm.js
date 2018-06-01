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

const optionFromObject = (obj, path, valueKey = 'id', labelKey = 'name') => {
  const item = path ? f.get(obj.path) : obj
  return { label: f.get(item, labelKey), value: f.get(item, valueKey) }
}

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

const prepareBuildingsRooms = ({ requests, rooms }) => {
  const roomsByBuildingId = f.groupBy(rooms, 'building.id')
  const buildings = f.uniqById(f.map(rooms, 'building'))
  return { buildings, roomsByBuildingId }
}

const RequestForm = ({ data, className, onClose }) => {
  const request = f.first(data.requests)
  const fields = prepareFormValues(request)
  const { buildings, roomsByBuildingId } = prepareBuildingsRooms(data)
  return (
    <ControlledForm idPrefix={`request_form_${request.id}`} values={fields}>
      {({ fields, ...formHelper }) => {
        // add auto-translated labels:
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
              alert(JSON.stringify(fields, 0, 2))
            }}
          >
            <Row>
              <Col lg>
                <FormField {...formPropsFor('article_name')} />

                <FormField {...formPropsFor('article_number')} />

                <FormField {...formPropsFor('supplier')} />

                <FormField {...formPropsFor('receiver')} autoComplete="name" />

                <FormGroup>
                  <Select
                    {...formPropsFor('building')}
                    options={buildings.map(b => optionFromObject(b))}
                  />
                </FormGroup>

                <FormGroup>
                  <Select
                    {...formPropsFor('room')}
                    options={f.map(roomsByBuildingId[fields.building], r =>
                      optionFromObject(r)
                    )}
                  />
                </FormGroup>

                <FormField type="textarea" {...formPropsFor('motivation')} />

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
                )}
              </Col>
            </Row>

            <hr m="mt-0" />

            <Row m="t-5">
              <Col lg>
                <button type="submit" className="btn m-1 btn-outline-dark">
                  <Icon.Exchange /> {t('form_btn_move_category')}
                </button>
                <button type="submit" className="btn m-1 btn-outline-dark">
                  <Icon.Calendar /> {t('form_btn_change_budget_period')}
                </button>
                <button type="submit" className="btn m-1 btn-outline-danger">
                  <Icon.Trash /> {t('form_btn_delete')}
                </button>
              </Col>
              <Col lg order="first" className="mt-3 mt-lg-0">
                <button type="submit" className="btn m-1 btn-primary">
                  <Icon.Checkmark /> <span>{t('form_btn_save')}</span>
                </button>
                <button
                  type="button"
                  className="btn m-1 btn-outline-secondary"
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
