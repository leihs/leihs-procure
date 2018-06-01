import React from 'react'
import PropTypes from 'prop-types'
import ButtonDropdown from 'reactstrap/lib/ButtonDropdown'
import DropdownToggle from 'reactstrap/lib/DropdownToggle'
import DropdownMenu from 'reactstrap/lib/DropdownMenu'
import DropdownItem from 'reactstrap/lib/DropdownItem'

function titleBySelection(selected, values) {
  const count = selected.length
  if (count === 0) {
    return 'Keine Ausgew\xE4hlt'
  }
  if (count > 3) {
    return `${count} selektiert`
  }
  return selected
    .map(item => values.filter(({ value }) => value === item)[0])
    .map(item => item && item.label)
    .join(', ')
}

class MultiSelect extends React.PureComponent {
  constructor() {
    super()
    this.state = { dropdownOpen: false, selected: [] }
    this.onCheckboxChange = this.onCheckboxChange.bind(this)
    this.onSelectAllChange = this.onSelectAllChange.bind(this)
  }

  componentDidMount() {
    const inititalSelected = this.props.values
      .filter(i => i.selected)
      .map(i => i.value)
    this.setState(() => ({ selected: inititalSelected }))
  }

  toggleDropdown() {
    this.setState(state => ({
      dropdownOpen: !state.dropdownOpen
    }))
  }

  onCheckboxChange(event, value) {
    const isSelected = event.target.checked
    const item = value || event.target.name
    this.setState(
      prev => {
        const selected = prev.selected
          .concat(item)
          .filter(val => (val === item ? isSelected : true))
        return { selected }
      },
      () => this.props.onChange(this.state.selected)
    )
  }

  onSelectAllChange(event) {
    const isSelected = event.target.checked
    this.setState((prev, props) => ({
      selected: !isSelected ? [] : props.values.map(({ id }) => id)
    }))
  }

  render({ props, state } = this) {
    const { values, name, ...restProps } = props
    const allSelected = values.length === state.selected.length

    return (
      <ButtonDropdown
        {...restProps}
        isOpen={this.state.dropdownOpen}
        toggle={() => this.toggleDropdown()}
      >
        <DropdownToggle caret outline>
          {titleBySelection(state.selected, values)}
        </DropdownToggle>
        <DropdownMenu className="multiselect-container">
          <DropdownItem
            className={'multiselect-all ' + (allSelected ? 'active' : '')}
          >
            <a tabIndex="0" className="multiselect-all">
              <label className="checkbox">
                <input
                  type="checkbox"
                  checked={allSelected}
                  onChange={this.onSelectAllChange}
                />
                {'Alle ausw\xE4hlen'}{' '}
              </label>
            </a>
          </DropdownItem>
          {values.map(({ value, label }, ix) => {
            const isSelected = state.selected.indexOf(value) !== -1
            return (
              // <DropdownItem key={ix} className={isSelected ? 'active' : ''}>
              <label
                key={ix}
                className="checkbox"
                data-htmlFor={`${value}-checkbox`}
              >
                <input
                  id={`${value}-checkbox`}
                  type="checkbox"
                  value={value}
                  checked={isSelected}
                  onChange={e => this.onCheckboxChange(e, value)}
                />
                {label}
                {' X '}
              </label>
              // {/* </DropdownItem> */}
            )
          })}
        </DropdownMenu>
      </ButtonDropdown>
    )
  }
}

MultiSelect.defaultProps = {
  name: '',
  values: [],
  onChange: selected => {
    // eslint-disable-next-line no-console
    console.log('MultiSelect', { selected })
  }
}

MultiSelect.propTypes = {
  name: PropTypes.string.isRequired,
  values: PropTypes.arrayOf(
    PropTypes.shape({
      value: PropTypes.string.isRequired,
      label: PropTypes.node.isRequired
    })
  ).isRequired
}

export default MultiSelect
