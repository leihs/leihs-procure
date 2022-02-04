import f from 'lodash'

import { DateTime } from 'luxon'
import { formatMoney } from 'accounting-js'

import * as CONSTANTS from '../constants'
import t from '../locale/translate'
import { isDev } from '../env'

export { default as IdentifierDecorator } from './IdentifierDecorator'

export const DisplayName = (o, { short = false, abbr = false } = {}) => {
  if (short && abbr) throw new Error('Invalid Options!')
  // NOTE: Checks *keys* must be present, but values can be missing.
  //       Guards against forgetting to query the keys/fields (via GraphQL)!
  const expectKeys = !isDev
    ? f.noop
    : wanted => {
        if (!isDev) return
        const missing = f.difference(wanted, Object.keys(o))
        if (missing.length > 0) throw new Error(`Missing keys! ${missing}`)
      }

  if (!o) return false

  switch (o.__typename) {
    case 'Room':
      expectKeys(['name', 'description'])
      return short || !o.description
        ? `${o.name}`
        : `${o.name} (${o.description})`

    case 'Organization':
      expectKeys(['name', 'shortname'])
      return short || !o.shortname
        ? `${o.shortname || o.name}`
        : `${o.name} (${o.shortname})`

    // TODO: check against DB/leihs core and schema
    case 'Model':
      expectKeys(['product', 'version'])
      return short || !o.version
        ? `${o.product}`
        : `${o.product} (${o.version})`

    case 'Supplier':
      expectKeys(['name'])
      return o.name

    case 'User':
      expectKeys(['firstname', 'lastname'])
      if (abbr)
        return `${o.firstname || ''} ${o.lastname || ''}`
          .split(/\W/)
          .filter(f.presence)
          .map(s => f.first(s).toUpperCase())
          .filter((s, i, a) => i < 2 || a.length - i <= 3)
          .join('')

      if (short)
        return `${f
          .filter([f.first(f.toUpper(o.firstname))])
          .concat('')
          .join('. ')}${o.lastname}`

      return `${o.firstname || ''} ${o.lastname || ''}`.trim()

    default:
      throw new Error(`DisplayName: unknown type '${o.__typename}'!`)
  }
}

export const budgetPeriodDates = bp => {
  const now = DateTime.local()
  const inspectStartDate = DateTime.fromISO(bp.inspection_start_date)
  const endDate = DateTime.fromISO(bp.end_date)
  const isPast = endDate <= now
  const isRequesting = !isPast && now <= inspectStartDate
  const isInspecting = !isPast && !isRequesting
  return { inspectStartDate, endDate, isPast, isRequesting, isInspecting }
}

export const RequestFieldValue = (name, request) => {
  const r = request
  switch (name) {
    case 'state':
      return t(`request_state_label_${r.state}`)
    case 'replacement':
      return t(
        `request_replacement_labels_${
          CONSTANTS.REQUEST_REPLACEMENT_VALUES_MAP[r.replacement.value]
        }`
      )
    case 'priority':
      return t(`priority_label_${r.priority.value}`)
    case 'inspector_priority':
      return t(`inspector_priority_label_${r.inspector_priority.value}`)
    case 'accounting_type':
      return t(
        `request_form_field.accounting_type_label_${r.accounting_type.value}`
      )
    case 'order_status':
      return t(
        `request_form_field.order_status_label_${r.accounting_type.value}`
      )
    default:
      throw new Error(`Unknown RequestField name '${name}'`)
  }
}

export const RequestTotalAmount = fields => {
  const allQuantities = ['requested', 'approved', 'order'].map(k => {
    const v = f.get(fields, [`${k}_quantity`])
    return f.isObject(v) ? v.value : v
  })
  const quantity = f.last(f.filter(allQuantities, f.present))

  const price_cents =
    f.get(fields, 'price_cents.value') || f.get(fields, 'price_cents') || '0'

  const price = parseInt(price_cents, 10)
  return (parseInt(quantity, 10) || 0) * price
}

// TODO: currency config, hardcoded to CH-de for now
// NOTE: `precision: 0` because Procure only supports integers
export const formatCurrency = (n = 0) => {
  const amount = n / 100
  if (!f.isNumber(amount) || f.isNaN(amount)) return '---'
  return formatMoney(amount, {
    decimal: '.',
    thousand: "'",
    symbol: 'CHF',
    format: '%v %s',
    precision: 0
  })
}
