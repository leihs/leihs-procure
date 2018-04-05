import React from 'react'

import t from '../locale/translate'
import Icon from './Icons'
import {
  Row,
  Col,
  FilePicker,
  FormGroup,
  FormField,
  Select,
  ButtonRadio
} from './Bootstrap'

import { ControlledForm } from './ReactUtils'
import { RequestTotalAmount as TotalAmount } from './decorators'

// dev
// import ROOMS_JSON from 'rooms.json'
const ROOMS_JSON = [{ id: 1, name: 'Raum 1' }, { id: 2, name: 'Raum 2' }]

const RequestForm = () => (
  <ControlledForm
    idPrefix="request_form"
    render={({ fields, formPropsFor }) => {
      return (
        <form
          id="request_form"
          className="XXXwas-validated"
          onSubmit={e => {
            e.preventDefault()
            alert(JSON.stringify(fields, 0, 2))
          }}>
          <Row>
            <Col lg>
              <FormField {...formPropsFor('article')} />

              <FormField {...formPropsFor('article_nr')} />

              <FormField {...formPropsFor('supplier')} />

              <FormField
                {...formPropsFor('receiver_name')}
                autoComplete="name"
              />

              <FormField {...formPropsFor('building')} />

              <FormField>
                <Select
                  {...formPropsFor('room')}
                  options={ROOMS_JSON.slice(0, 100).map(({ id, name }) => ({
                    value: id,
                    label: name
                  }))}
                />
              </FormField>

              <FormField {...formPropsFor('purpose')} />

              <Row>
                <Col sm>
                  <FormField>
                    <Select
                      {...formPropsFor('priority_requester')}
                      options={[0, 1, 2, 3].map(n => ({
                        value: n,
                        label: t(`field.request_priority_inspector_labels.${n}`)
                      }))}
                    />
                  </FormField>
                </Col>
                <Col sm>
                  <FormField>
                    <Select
                      {...formPropsFor('priority_inspector')}
                      options={[0, 1, 2, 3].map(n => ({
                        value: n,
                        label: t(`field.request_priority_inspector_labels.${n}`)
                      }))}
                    />
                  </FormField>
                </Col>
              </Row>

              <FormField>
                <ButtonRadio
                  {...formPropsFor('replacement')}
                  options={['replacement', 'new'].map(k => ({
                    value: k,
                    label: t(`field.request_replacement_labels_${k}`)
                  }))}
                />
              </FormField>
            </Col>

            <Col lg>
              <Row>
                <Col sm>
                  <FormField
                    {...formPropsFor('price')}
                    type="number-integer"
                    labelSmall={t('field.price_help')}
                    helpText="Bitte nur ganze Zahlen eingeben"
                  />
                </Col>
                <Col sm>
                  <FormField
                    type="text-static"
                    name="price_total"
                    value={TotalAmount(fields)}
                    label={t('field.price_total')}
                    labelSmall={t('field.price_help')}
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
                {...formPropsFor('comment_inspector')}
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

              <FormGroup label={t('field.attachments')}>
                <FilePicker id="attachments" name="attachments" />
              </FormGroup>

              <FormField>
                <ButtonRadio
                  {...formPropsFor('accounting_type')}
                  options={['aquisition', 'investment'].map(k => ({
                    value: k,
                    label: t(`field.accounting_type_label_${k}`)
                  }))}
                />
              </FormField>

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
              <button type="submit" className="btn m-1 btn-outline-secondary">
                {t('form_btn_cancel')}
              </button>
            </Col>
          </Row>
          <pre>{JSON.stringify({ fields }, 0, 2)}</pre>
        </form>
      )
    }}
  />
)

export default RequestForm
