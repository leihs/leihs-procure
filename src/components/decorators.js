import f from 'lodash'

export const DisplayName = o => {
  switch (o.__typename) {
    case 'User':
      return `${o.firstname} ${o.lastname}`
    default:
      throw new Error(`DisplayName: unknown type '${o.__typename}'!`)
  }
}

export const RequestTotalAmount = fields => {
  const quantity = f.last(
    f.filter(
      ['requested', 'approved', 'ordered'].map(k => fields[`quantity_${k}`])
    )
  )
  const price = (parseInt(fields.price_cents, 10) || 0) / 100
  return (parseInt(quantity, 10) || 0) * price
}
