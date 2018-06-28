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

    # FIXME: price fields
    # price_cents {
    #   value
    # }
    # price_currency {
    #   value
    # }

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

    # FIXME: state
    # state {
    #   value
    # }
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

    # FIXME: price fields
    # price_cents {
    #   ...RequestFieldString
    # }
    # price_currency {
    #   ...RequestFieldString
    # }

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
    #   ...RequestFieldString
    # }
    # FIXME: state
    # state {
    #   ...RequestFieldString
    # }

    article_number {
      ...RequestFieldString
    }
    motivation {
      ...RequestFieldString
    }

    # FIXME: replacement
    # replacement {
    #   ...RequestFieldBoolean
    # }

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

    # FIXME: accounting_type
    # accounting_type {
    #   ...RequestFieldString
    # }
    # internal_order_id
  }
  ${RequestFieldsForIndex}
  ${RequestField.String}
  ${RequestField.Int}
  #{RequestField.Boolean}
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
