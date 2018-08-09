import f from 'lodash'
import { DateTime } from 'luxon'
import { formatMoney } from 'accounting-js'

const isDev = process.env.NODE_ENV !== 'production'
const noop = () => {}

export const DisplayName = (o, { short = false, abbr = false } = {}) => {
  if (short && abbr) throw new Error('Invalid Options!')
  // NOTE: Checks *keys* must be present, but values can be missing.
  //       Guards against forgetting to query the keys/fields (via GraphQL)!
  const expectKeys = isDev
    ? noop
    : wanted => {
        if (!isDev) return
        const missing = f.difference(wanted, Object.keys(o))
        if (missing.length > 0) throw new Error(`Missing keys! ${missing}`)
      }

  if (!o) return '?'

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

    case 'Supplier':
      expectKeys(['name'])
      return o.name

    case 'User':
      expectKeys(['firstname', 'lastname'])
      if (abbr)
        return `${o.firstname || ''} ${o.lastname || ''}`
          .split(/\W/)
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
export const formatCurrency = (n = 0) =>
  formatMoney(n / 100, {
    decimal: '.',
    thousand: "'",
    symbol: 'CHF',
    format: '%v %s',
    precision: 0
  })
