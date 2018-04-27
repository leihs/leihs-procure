import React from 'react'
import Downshift from 'downshift'
import { Query } from 'react-apollo'
import gql from 'graphql-tag'

const SEARCH_USERS_QUERY = gql`
  query($searchTerm: String!) {
    users(search_term: $searchTerm) {
      id
      firstname
      lastname
    }
  }
`

const UserList = ({ users, highlightedIndex, selectedItem, getItemProps }) => (
  <div>
    {users.slice(0, 10).map(({ id, firstname, lastname }, index) => {
      const item = `${firstname} ${lastname}`
      return (
        <div
          {...getItemProps({ item })}
          key={id}
          style={{
            backgroundColor: highlightedIndex === index ? 'gray' : 'white',
            fontWeight: selectedItem === item ? 'bold' : 'normal'
          }}
        >
          {item}
        </div>
      )
    })}
  </div>
)

const UserListWithData = ({ searchTerm, ...props }) => (
  <Query query={SEARCH_USERS_QUERY} variables={{ searchTerm }}>
    {({ loading, error, data }) => {
      if (loading) return <p>Loading...</p>
      if (error)
        return (
          <p>
            Error :( <code>{error.toString()}</code>
          </p>
        )

      return <UserList users={data.users} {...props} />
    }}
  </Query>
)

const UserAutocomplete = ({ onChange }) => (
  <Downshift onChange={onChange}>
    {({
      getInputProps,
      getItemProps,
      isOpen,
      inputValue,
      selectedItem,
      highlightedIndex
    }) => (
      <div>
        <input {...getInputProps({ placeholder: 'Search' })} />
        {isOpen ? (
          <div style={{ border: '1px solid #ccc' }}>
            <UserListWithData
              searchTerm={inputValue}
              selectedItem={selectedItem}
              highlightedIndex={highlightedIndex}
              getItemProps={getItemProps}
            />
          </div>
        ) : null}
      </div>
    )}
  </Downshift>
)

export default UserAutocomplete
