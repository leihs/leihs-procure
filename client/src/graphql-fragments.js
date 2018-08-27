import gql from 'graphql-tag'

const scalarField = t => gql`
  fragment RequestField${t} on RequestField${t} { value, read, write, required }
`

export const RequestField = {
  String: scalarField('String'),
  Int: scalarField('Int'),
  Boolean: scalarField('Boolean')
}

export const RequestFieldsForIndex = gql`
  fragment RequestFieldsForIndex on Request {
    id

    user {
      value {
        id
        firstname
        lastname
      }
    }

    category {
      value {
        id
        name
        # FIXME: GraphQL error: db-spec null is missing a required parameter
        # main_category {
        #   id
        # }
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
    model {
      value {
        id
        product
        version
      }
    }

    receiver {
      value
    }
    organization {
      value {
        id
        name
        shortname
      }
    }

    price_cents {
      value
    }
    price_currency {
      value
    }

    requested_quantity {
      read
      value
    }
    approved_quantity {
      read
      value
    }
    order_quantity {
      read
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

export const RequestFieldsForEdit = gql`
  fragment RequestFieldsForEdit on Request {
    ...RequestFieldsForIndex

    template {
      value {
        id
        article_name
      }
    }

    user {
      read
      write
      required
      value {
        id
        firstname
        lastname
      }
    }

    category {
      read
      write
      required
      value {
        id
        name
      }
    }
    budget_period {
      read
      write
      required
      value {
        id
      }
    }

    article_name {
      ...RequestFieldString
    }
    model {
      read
      write
      required
      value {
        id
        product
        version
      }
    }

    supplier_name {
      ...RequestFieldString
    }
    supplier {
      read
      write
      required
      value {
        id
        name
      }
    }
    receiver {
      ...RequestFieldString
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

    priority {
      read
      write
      required
      value
    }
    inspector_priority {
      read
      write
      required
      value
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
      required
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

    attachments {
      read
      write
      required
      value {
        id
        filename
        url
      }
    }

    accounting_type {
      ...RequestFieldString
    }
    cost_center {
      read
      write
      required
      value
    }
    procurement_account {
      read
      write
      value
      # TODO: for this field a Bool is not enough, its a dependent field!
      # required
    }
    general_ledger_account {
      read
      value
      # NOTE: it is always read-only (or hidden)
      # write
      # required
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
      shortname
    }
    department {
      id
      name
    }
  }
`
