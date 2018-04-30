import React from 'react'
import cx from 'classnames'
import Downshift from 'downshift'
import { Query } from 'react-apollo'

const defaultProps = {}

const highlightTextParts = (highlight, text) => {
  // TODO: remove non-letter characters (replace with dot match in regex)
  // TODO: split term by whitespace and mark all the parts
  const part = highlight

  // match parts case insensitive!
  const textParts = text.split(new RegExp(part, 'i')).reduce(
    (m, i) => {
      // preserve case, get match back from original text!
      const position = m.position + i.length + part.length
      const origMatch = text.slice(position).slice(0, part.length)
      return { position, parts: m.parts.concat([i, origMatch]) }
    },
    { position: -part.length, parts: [] }
  )['parts']

  const matcher = new RegExp(part, 'i')
  let txt = text
  const res = []
  // debugger
  while (txt) {
    const matched = matcher.exec(txt)
    if (!matched) {
      res.push(txt)
      txt = ''
      break
    }
    const start = text.slice(0, matched.index)
    res.push(start, matched[0])
    const rest = text.slice(matched[0].length + matched.index)
    txt = rest
    console.log(txt)
  }

  return (
    <React.Fragment>
      {textParts.map(
        (str, ix) =>
          // every even element is an higlight!
          ix % 2 ? <b>{str}</b> : str
      )}
    </React.Fragment>
  )
}

const ItemsList = ({
  items,
  searchTerm,
  highlightedIndex,
  getItemProps,
  itemToString,
  idFromItem = ({ id }) => id
}) => {
  return (
    <div className="border rounded w-100 mt-1">
      {items.map((item, index) => (
        <div
          {...getItemProps({ item: idFromItem(item) })}
          key={idFromItem(item)}
          className={cx('px-3 py-2', {
            'rounded-top': index === 0,
            'bg-dark text-light': highlightedIndex === index
          })}
        >
          {highlightTextParts(searchTerm, itemToString(item))}
        </div>
      ))}
    </div>
  )
}

const FetchItemsList = ({ searchQuery, searchTerm, ...props }) => (
  <Query query={searchQuery} variables={{ searchTerm }}>
    {({ loading, error, data }) => {
      if (loading) return <p>Loading...</p>
      if (error)
        return (
          <p>
            Error :( <code>{error.toString()}</code>
          </p>
        )

      return <ItemsList items={data.users} searchTerm={searchTerm} {...props} />
    }}
  </Query>
)

const InlineSearch = ({
  searchQuery,
  itemToString,
  idFromItem,
  onSelect,
  ...props
}) => (
  <Downshift
    {...props}
    onSelect={(selectedItem, instance) => {
      if (!selectedItem) return
      onSelect && onSelect(selectedItem)
      instance.clearSelection()
    }}
  >
    {({
      getInputProps,
      getItemProps,
      isOpen,
      inputValue,
      selectedItem,
      highlightedIndex
    }) => (
      <div>
        <input
          {...getInputProps({ placeholder: 'Search' })}
          className="form-control"
        />
        {isOpen ? (
          <FetchItemsList
            searchQuery={searchQuery}
            searchTerm={inputValue}
            itemToString={itemToString}
            idFromItem={idFromItem}
            selectedItem={selectedItem}
            highlightedIndex={highlightedIndex}
            getItemProps={getItemProps}
          />
        ) : null}
      </div>
    )}
  </Downshift>
)

export default InlineSearch
