import React from 'react'
import PropTypes from 'prop-types'
import {
  ButtonDropdown,
  DropdownToggle,
  DropdownMenu,
  DropdownItem
} from 'reactstrap'

function titleBySelection(selected, values) {
  const count = selected.length
  if (count === 0) {
    return 'Keine Ausgew\xE4hlt'
  }
  if (count > 3) {
    return `${count} selektiert`
  }
  return selected
    .map(item => values.filter(({ id }) => id === item)[0].label)
    .join(', ')
}

class MultiSelect extends React.Component {
  constructor() {
    super()
    this.state = { dropdownOpen: false, selected: [] }
    this.onCheckboxChange = this.onCheckboxChange.bind(this)
    this.onSelectAllChange = this.onSelectAllChange.bind(this)
  }

  componentDidMount() {
    const inititalSelected = this.props.values
      .filter(i => i.selected)
      .map(i => i.id)
    this.setState(() => ({ selected: inititalSelected }))
  }

  toggleDropdown() {
    this.setState(state => ({
      dropdownOpen: !state.dropdownOpen
    }))
  }

  onCheckboxChange(event) {
    const isSelected = event.target.checked
    const item = event.target.name
    this.setState(prev => {
      const selected = prev.selected
        .concat(item)
        .filter(id => (id === item ? isSelected : true))
      return { selected }
    })
  }

  onSelectAllChange(event) {
    const isSelected = event.target.checked
    this.setState((prev, props) => ({
      selected: !isSelected ? [] : props.values.map(({ id }) => id)
    }))
  }

  render({ props, state } = this) {
    const { values, ...restProps } = props
    const allSelected = values.length === state.selected.length

    return (
      <ButtonDropdown
        {...restProps}
        isOpen={this.state.dropdownOpen}
        toggle={() => this.toggleDropdown()}>
        <DropdownToggle caret>
          {titleBySelection(state.selected, values)}
        </DropdownToggle>
        <DropdownMenu className="multiselect-container">
          <DropdownItem
            className={'multiselect-all ' + (allSelected ? 'active' : '')}>
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
          {values.map(({ id, label }) => {
            const isSelected = state.selected.indexOf(id) !== -1
            return (
              <DropdownItem key={id} className={isSelected ? 'active' : ''}>
                <a tabIndex="0">
                  <label className="checkbox">
                    <input
                      type="checkbox"
                      name={id}
                      checked={isSelected}
                      onChange={this.onCheckboxChange}
                    />
                    {label}{' '}
                  </label>
                </a>
              </DropdownItem>
            )
          })}
        </DropdownMenu>
      </ButtonDropdown>
    )
  }
}

MultiSelect.defaultProps = { name: '', values: [] }

MultiSelect.propTypes = {
  name: PropTypes.string.isRequired,
  values: PropTypes.arrayOf(
    PropTypes.objectOf({
      id: PropTypes.string.isRequired,
      label: PropTypes.node.isRequired
    })
  ).isRequired
}

export default MultiSelect
