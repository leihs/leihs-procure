import gql from 'graphql-tag'

const scalarField = t => gql`
  fragment RequestField${t} on RequestField${t} { value, read, write }
`

export const RequestField = {
  String: scalarField('String'),
  Int: scalarField('Int'),
  Boolean: scalarField('Boolean')
}

export const RequestFieldsForIndex = gql`
  fragment RequestFieldsForIndex on Request {
    id

    category {
      id
      name
    }
    budget_period {
      id
    }

    article_name {
      value
    }
    receiver {
      value
    }
    organization {
      id
    }

    price_cents {
      value
    }
    price_currency {
      value
    }

    requested_quantity {
      value
    }
    approved_quantity {
      value
    }
    order_quantity {
      value
    }

    replacement {
      value
    }

    priority {
      value
    }
    state {
      value
    }
  }
`

export const RequestFieldsForShow = gql`
  fragment RequestFieldsForShow on Request {
    ...RequestFieldsForIndex

    category {
      id
      name
    }
    budget_period {
      id
    }

    article_name {
      ...RequestFieldString
    }
    receiver {
      ...RequestFieldString
    }
    organization {
      id
    }

    price_cents {
      ...RequestFieldString
    }
    price_currency {
      ...RequestFieldString
    }

    requested_quantity {
      ...RequestFieldInt
    }
    approved_quantity {
      ...RequestFieldInt
    }
    order_quantity {
      ...RequestFieldInt
    }

    priority {
      ...RequestFieldString
    }
    state {
      ...RequestFieldString
    }

    article_number {
      ...RequestFieldString
    }
    motivation {
      ...RequestFieldString
    }

    replacement {
      ...RequestFieldBoolean
    }

    room {
      read
      write
      value {
        id
        name
        building {
          id
          name
        }
      }
    }

    inspection_comment {
      ...RequestFieldString
    }
    # TODO: priority_inspector

    # TODO: attachments

    accounting_type {
      ...RequestFieldString
    }
    # internal_order_id
  }
  ${RequestFieldsForIndex}
  ${RequestField.String}
  ${RequestField.Int}
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
