import gql from 'graphql-tag'

export const RequestFieldsForIndex = gql`
  fragment RequestFieldsForIndex on Request {
    id
    category {
      id
      name
    }
    budget_period_id

    article_name
    receiver
    organization_id

    price_cents
    price_currency

    requested_quantity
    approved_quantity
    order_quantity

    priority
    state
  }
`

export const RequestFieldsForShow = gql`
  fragment RequestFieldsForShow on Request {
    ...RequestFieldsForIndex

    article_number
    motivation
    building_id
    room_id

    inspection_comment
    # TODO: priority_inspector

    # TODO: attachments

    accounting_type
    internal_order_id
  }
  ${RequestFieldsForIndex}
`

export const RequesterOrg = gql`
  fragment RequesterOrg on RequesterOrganization {
    id
    user {
      id
      firstname
      lastname
    }
    organization {
      id
      name
    }
    department {
      id
      name
    }
  }
`
