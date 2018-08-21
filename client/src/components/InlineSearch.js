import React from 'react'
import PropTypes from 'prop-types'
import f from 'lodash'
import cx from 'classnames'
import Downshift from 'downshift'
import { Query } from 'react-apollo'
import Highlighter from 'react-highlight-words'
// TODO: pgUnaccent
// import pgUnaccent from 'postgres-unaccent'
import pgUnaccent from 'lodash/deburr'
import logger from 'debug'
const log = logger('app:ui:InlineSearch')

// # COMPONENT
const defaultProps = {
  itemToString: item => String(item)
}
const propTypes = {
  onSelect: PropTypes.func,
  onChange: PropTypes.func
}

const InlineSearch = ({
  searchQuery,
  queryVariables,
  itemToString,
  idFromItem,
  onSelect,
  onChange,
  inputProps,
  value,
  required,
  ...props
}) => {
  log('render', props)
  return (
    <Downshift
      {...props}
      selectedItem={value}
      itemToString={i => itemToString(i) || ''}
      onSelect={(selectedItem, instance) => {
        log('onSelect', selectedItem)
        if (!selectedItem) return
        onSelect && onSelect(selectedItem)
        onChange &&
          onChange({ target: { name: props.name, value: selectedItem } })
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
            onFocus={e => e.target.select()}
            {...getInputProps({
              required,
              placeholder: 'Search',
              ...inputProps
            })}
            {...inputNodeVisualProps}
          />
          <Query
            skip={!isOpen || f.isEmpty(inputValue)}
            query={searchQuery}
            variables={{ ...queryVariables, searchTerm: inputValue }}
          >
            {({ loading, error, data }) => {
              if (!isOpen) return false
              log('query', { loading, error, data })
              if (loading) return <EmptyResult>Loading...</EmptyResult>
              if (error) {
                log(error)
                const errMsg =
                  !error || f.isEmpty(error.graphQLErrors)
                    ? error.toString()
                    : JSON.stringify(error.graphQLErrors, 0, 2)
                return (
                  <EmptyResult>
                    Error :( <code>{errMsg}</code>
                  </EmptyResult>
                )
              }

              const resultKeys = f.keys(data)
              if (resultKeys.length !== 1) throw new Error('Ambiguous result!')
              const items = data[resultKeys[0]]

              return (
                <ItemsList
                  items={items}
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
InlineSearch.propTypes = propTypes

export default InlineSearch

// # STYLES
const UICLASS = 'ui-interactive-text-field'
const itemHeight = 2 // em, min. height (line can wrap currently!)
const resultsItemVisualProps = { className: 'px-2 py-1' }

// // TODO: non-compact variant:
// const itemHeight = 2.5 // em, min. height (line can wrap currently!)
// const resultsItemVisualProps = { className: 'px-3 py-2' }

const inputNodeVisualProps = {
  className: cx(UICLASS, `${UICLASS}-inputnode`, 'form-control'),
  style: { position: 'relative' }
}
const resultsWrapperVisualProps = {
  style: { position: 'relative' }
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
      {f.isEmpty(items) ? (
        <div {...resultsItemVisualProps}>No results!</div>
      ) : (
        items.map((item, index) => (
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
        ))
      )}
    </div>
  )
}

const EmptyResult = ({ children }) => {
  return (
    <div {...resultsBoxVisualProps}>
      <div {...resultsItemVisualProps}>{children}</div>
    </div>
  )
}
