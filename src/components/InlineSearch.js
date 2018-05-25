import React from 'react'
import cx from 'classnames'
import isEmpty from 'lodash/isEmpty'
import Downshift from 'downshift'
import { Query } from 'react-apollo'
import Highlighter from 'react-highlight-words'
// TODO: pgUnaccent
// import pgUnaccent from 'postgres-unaccent'
import pgUnaccent from 'lodash/deburr'
import logger from 'debug'
const log = logger('app:ui:InlineSearch')

// # STYLES
const UICLASS = 'ui-interactive-text-field'
const itemHeight = 2 // em, min. height (line can wrap currently!)
const resultsItemVisualProps = { className: 'px-2 py-1' }

// // TODO: non-compact variant:
// const itemHeight = 2.5 // em, min. height (line can wrap currently!)
// const resultsItemVisualProps = { className: 'px-3 py-2' }

const inputNodeVisualProps = {
  className: cx(UICLASS, `${UICLASS}-inputnode`, 'form-control'),
  style: { position: 'relative', zIndex: 2 }
}
const resultsWrapperVisualProps = {
  style: {
    position: 'relative',
    zIndex: 2
  }
}
const resultsBoxVisualProps = {
  className: cx(UICLASS, `${UICLASS}-results`, 'border rounded w-100 mt-1'),
  style: {
    position: 'absolute',
    left: 0,
    zIndex: 1,
    maxHeight: 5.5 * itemHeight + 'em',
    overflowX: 'hidden',
    overflowY: 'scroll',
    background: 'var(--content-bg-color)',
    boxShadow: '0 0.4rem 0.8rem rgba(0,0,0,.125)',
    marginTop: '-1px !important'
  }
}

// # HELPERS

const highlightTextParts = (highlight, text) => {
  return (
    <Highlighter
      textToHighlight={text}
      searchWords={highlight.split(/\s+/g)}
      sanitize={s => pgUnaccent(s).toLowerCase()}
      highlightTag="mark"
      highlightClass={cx(UICLASS, `${UICLASS}-result-item-highlight`)}
      highlightStyle={{
        margin: 0,
        padding: 0,
        background: 'inherit',
        color: 'inherit',
        fontWeight: 'bold'
      }}
      autoEscape={true}
    />
  )
}

// # UI PARTIALS
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
          {...getItemProps({ item: item })}
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

// # COMPONENT
const defaultProps = {
  itemToString: item => String(item)
}

const InlineSearch = ({
  searchQuery,
  queryVariables,
  itemToString,
  idFromItem,
  onSelect,
  ...props
}) => {
  log('render', props)
  return (
    <Downshift
      {...props}
      itemToString={i => itemToString(i) || ''}
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
        <div
          className={cx(UICLASS, 'ui-inline-search')}
          {...resultsWrapperVisualProps}
        >
          <input
            value=""
            onChange={() => {}}
            {...getInputProps({ placeholder: 'Search' })}
            {...inputNodeVisualProps}
          />
          <Query
            skip={!isOpen || isEmpty(inputValue)}
            query={searchQuery}
            variables={{ ...queryVariables, searchTerm: inputValue }}
          >
            {({ loading, error, data }) => {
              if (!isOpen) return false
              if (loading) return <p>Loading...</p>
              if (error) {
                log(error)
                const errMsg =
                  !error || isEmpty(error.graphQLErrors)
                    ? error.toString()
                    : JSON.stringify(error.graphQLErrors, 0, 2)
                return (
                  <p>
                    Error :( <code>{errMsg}</code>
                  </p>
                )
              }

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
        </div>
      )}
    </Downshift>
  )
}

InlineSearch.defaultProps = defaultProps

export default InlineSearch
