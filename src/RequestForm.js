import React, { Component } from 'react'
import cx from 'classnames'
import f from 'lodash'

import Icon from './Icons'

// debugger

// dev
window.f = f

const t = key => {
  const translations = {
    form_btn_save: 'Speichern',
    form_btn_cancel: 'Abbrechen',
    form_btn_move_category: 'Kategorie ändern',
    form_btn_change_budget_period: 'Budgetperiode ändern',
    form_btn_delete: 'Löschen',
    request_state: 'Status',
    field: {
      article: 'Artikel oder Projekt',
      article_nr: 'Artikelnr. oder Herstellernr.',
      supplier: 'Lieferant',
      receiver_name: 'Name des Empfängers',
      building: 'Gebäude',
      room: 'Raum',
      purpose: 'Begründung',
      priority_requester: 'Priorität',
      priority_inspector: 'Priorität des Prüfers',
      is_new: 'Ersatz / Neu',
      price: 'Stückpreis CHF',
      price_help: 'inkl. MwSt',
      quantity_requested: 'Menge beantragt',
      quantity_approved: 'Menge bewilligt',
      quantity_ordered: 'Bestellmenge',
      price_total: 'Total CHF',
      price_total_help: 'inkl. MwSt',
      comment_inspector: 'Kommentar des Prüfers',
      attachments: 'Anhänge',
      accounting_type: 'Abrechnungsart',
      cost_center: 'Kostenstelle',
      general_ledger_account: 'Sachkonto',
      internal_order_number: 'Innenauftrag'
    },
    accounting_type_investment: 'Investition',
    accounting_type_aquisition: 'Beschaffung'
  }
  return f.get(translations, key) || `[${key}]`
}

const F = React.Fragment
const Div = ({ cls, className, ...props }) => <div className={cx(cls, className)} {...props} />
const Row = props => <Div {...props} cls="row" />
const Col = props => {
  const sizes = ['sm', 'md', 'lg', 'xl']
  const restProps = f.omit(props, sizes)
  const size = f.first(f.intersection(f.keys(props), sizes))
  const cls = size ? `col-${size}` : 'col'
  return <Div {...restProps} cls={cls} />
}

const FormGroup = ({ id, label, helpText, children, ...props }) => {
  return (
    <Div {...props} cls="form-group">
      <label htmlFor={id}>{label}</label>
      {children}
      {helpText && (
        <small id={`${id}--Help`} className="form-text text-muted">
          {helpText}
        </small>
      )}
    </Div>
  )
}

const TextField = ({
  id,
  label,
  labelSmall,
  type = 'text',
  name,
  placeholder,
  helpText,
  input,
  ...restInputProps
}) => {
  if (!id) id = name
  if (!name) name = id
  if (!label) label = t(`field.${name}`)

  const inputNode = input || (
    <input
      type={type}
      id={id}
      name={name}
      placeholder={placeholder}
      aria-describedby={helpText ? `${id}--Help` : null}
      className="form-control"
      {...restInputProps}
    />
  )

  const labelContent = (
    <F>
      {label}
      {labelSmall && (
        <F>
          {' '}
          <small>{labelSmall}</small>
        </F>
      )}
    </F>
  )

  return (
    <FormGroup label={labelContent} helpText={helpText}>
      {inputNode}
    </FormGroup>
  )
}

class RequestForm extends Component {
  render() {
    return (
      <form>
        <Row>
          <Col sm>
            <TextField name="article" />

            <TextField name="article_nr" />

            <TextField name="supplier" />

            <TextField name="receiver_name" />

            <TextField name="building" />

            <TextField name="room" />

            <TextField name="purpose" />

            <TextField name="priority_requester" />

            <TextField name="priority_inspector" />

            <TextField name="is_new" />
          </Col>

          <Col sm>
            <TextField name="price" labelSmall={t('field.price_help')} />

            <TextField name="quantity_requested" helpText="Bitte nur ganze Zahlen eingeben" />

            <TextField name="quantity_approved" />

            <TextField name="quantity_ordered" />

            <TextField name="price_total" labelSmall={t('field.price_total_help')} />

            <TextField name="comment_inspector" />

            <TextField name="attachments" />

            <FormGroup label={t('field.attachments')}>
              <input />
            </FormGroup>

            <TextField name="accounting_type" />

            <TextField name="cost_center" />

            <TextField name="general_ledger_account" />

            <TextField name="internal_order_number" />
          </Col>
        </Row>
        <Row>
          <Col sm>
            <button type="submit" className="btn m-1 btn-primary">
              <Icon.Checkmark /> <span>{t('form_btn_save')}</span>
            </button>
            <button type="submit" className="btn m-1 btn-outline-secondary">
              {t('form_btn_cancel')}
            </button>
          </Col>
          <Col sm className="mt-3 mt-sm-0">
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
        </Row>
      </form>
    )
  }
}

export default RequestForm
