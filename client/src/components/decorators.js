import f from 'lodash'

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
      ['requested', 'approved', 'ordered'].map(
        k =>
          f.get(fields, [`quantity_${k}`, 'value']) ||
          f.get(fields, [`quantity_${k}`])
      )
    )
  )
  const price = (parseInt(fields.price_cents, 10) || 0) / 100
  return (parseInt(quantity, 10) || 0) * price
}
