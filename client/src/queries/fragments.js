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
      value {
        id
        name
      }
    }
    budget_period {
      value {
        id
      }
    }

    article_name {
      value
    }
    receiver {
      value
    }
    organization {
      value {
        id
      }
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

    # FIXME: priority
    # priority {
    #   value
    # }

    state {
      value
    }
  }
`

export const RequestFieldsForShow = gql`
  fragment RequestFieldsForShow on Request {
    ...RequestFieldsForIndex

    category {
      value {
        id
        name
      }
    }
    budget_period {
      value {
        id
      }
    }

    article_name {
      ...RequestFieldString
    }
    supplier {
      read
      write
      value {
        id
        name
      }
    }
    supplier_name {
      ...RequestFieldString
    }
    receiver {
      ...RequestFieldString
    }
    organization {
      value {
        id
      }
    }

    price_cents {
      ...RequestFieldInt
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

    # FIXME: priority
    # priority {
    #   ...RequestFieldInt
    # }

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

    # priority_inspector {
    #   ...RequestFieldInt
    # }

    # TODO: attachments

    accounting_type {
      ...RequestFieldString
    }
    cost_center {
      read
      write
      value
    }
    procurement_account {
      read
      write
      value
    }
    internal_order_number {
      ...RequestFieldString
    }
  }
  ${RequestFieldsForIndex}
  ${RequestField.String}
  ${RequestField.Int}
  ${RequestField.Boolean}
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
