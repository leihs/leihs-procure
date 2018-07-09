import f from 'lodash'
import { formatMoney } from 'accounting-js'

export const DisplayName = (o, short = false) => {
  if (!o) return

  switch (o.__typename) {
    case 'User':
      return `${o.firstname} ${o.lastname}`

    case 'Room':
      return short || !o.description
        ? `${o.name}`
        : `${o.name} (${o.description})`

    case 'Organization':
      return short || !o.shortname
        ? `${o.shortname || o.name}`
        : `${o.name} (${o.shortname})`

    default:
      throw new Error(`DisplayName: unknown type '${o.__typename}'!`)
  }
}

export const RequestTotalAmount = fields => {
  const quantity = f.last(
    f.filter(
      ['requested', 'approved', 'order'].map(
        k =>
          f.get(fields, [`${k}_quantity`, 'value']) ||
          f.get(fields, [`${k}_quantity`])
      )
    )
  )
  const price = (parseInt(fields.price_cents, 10) || 0) / 100
  return (parseInt(quantity, 10) || 0) * price
}

// TODO: currency config, hardcoded to CH-de for now
// NOTE: `precision: 0` because Procure only supports integers
export const formatCurrency = n =>
  formatMoney(n, {
    decimal: '.',
    thousand: "'",
    symbol: 'CHF',
    format: '%v %s',
    precision: 0
  })
