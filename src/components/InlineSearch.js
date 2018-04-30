import React from 'react'
import cx from 'classnames'
import Downshift from 'downshift'
import { Query } from 'react-apollo'

// # STYLES
const itemHeight = 2 // em, min. height (line can wrap currently!)
const resultsItemVisualProps = { className: 'px-2 py-1' }

// // non-compact variant:
// const itemHeight = 2.5 // em, min. height (line can wrap currently!)
// const resultsItemVisualProps = { className: 'px-3 py-2' }

const resultsWrapperVisualProps = {
  style: {
    position: 'relative',
    zIndex: 2
  }
}
const inputNodeVisualProps = {
  className: 'form-control',
  style: { position: 'relative', zIndex: 2 }
}
const resultsBoxVisualProps = {
  className: 'border rounded w-100 mt-1',
  style: {
    position: 'absolute',
    left: 0,
    zIndex: 1,
    maxHeight: 5.5 * itemHeight + 'em',
    overflowX: 'hidden',
    overflowY: 'scroll',
    background: 'var(--content-bg-color)',
    boxShadow: '0 0.4rem 0.8rem rgba(0,0,0,.125)',
    'margin-top': '-1px !important'
  }
}

const defaultProps = {
  itemToString: item => String(item)
}

const highlightTextParts = (highlight, text) => {
  // TODO: remove non-letter characters (replace with dot match in regex)
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

  // // TODO: split term by whitespace and mark all the parts
  // const matcher = new RegExp(part, 'i')
  // let txt = text
  // const res = []
  // // debugger
  // let i = 0
  // while (txt && i < 1000) {
  //   const matched = matcher.exec(txt)
  //   if (!matched) {
  //     res.push(txt)
  //     txt = ''
  //     break
  //   }
  //   const start = txt.slice(0, matched.index)
  //   res.push(start, matched[0])
  //   const rest = txt.slice(matched[0].length + matched.index)
  //   txt = rest
  //   i++
  //   console.log({ txt, rest, i, matched })
  // }

  return (
    <React.Fragment>
      {textParts.map((str, ix) => (
        // every even element is an higlight!
        <React.Fragment key={ix}>{ix % 2 ? <b>{str}</b> : str}</React.Fragment>
      ))}
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
    <div {...resultsBoxVisualProps}>
      {items.length === 0 && <div {...resultsItemVisualProps}>No results!</div>}
      {items.map((item, index) => (
        <div
          {...getItemProps({ item: idFromItem(item) })}
          key={idFromItem(item)}
          {...resultsItemVisualProps}
          className={cx(resultsItemVisualProps.className, {
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

const InlineSearch = ({
  searchQuery,
  queryVariables,
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
      <div className="ui-inline-search" {...resultsWrapperVisualProps}>
        <input
          {...getInputProps({ placeholder: 'Search' })}
          {...inputNodeVisualProps}
        />
        {isOpen ? (
          <Query
            query={searchQuery}
            variables={{ ...queryVariables, searchTerm: inputValue }}
          >
            {({ loading, error, data }) => {
              if (loading) return <p>Loading...</p>
              if (error)
                return (
                  <p>
                    Error :( <code>{error.toString()}</code>
                  </p>
                )

              return (
                <ItemsList
                  items={data.users}
                  searchTerm={inputValue}
                  itemToString={itemToString}
                  idFromItem={idFromItem}
                  selectedItem={selectedItem}
                  highlightedIndex={highlightedIndex}
                  getItemProps={getItemProps}
                  {...props}
                />
              )
            }}
          </Query>
        ) : null}
      </div>
    )}
  </Downshift>
)

InlineSearch.defaultProps = defaultProps

export default InlineSearch
