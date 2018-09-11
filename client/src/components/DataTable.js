import React from 'react'
import cx from 'classnames'

class DataTable extends React.Component {
  render(
    {
      cols,
      rows,
      small,
      dark,
      darkHead,
      lightHead,
      bordered,
      striped,
      hover
    } = this.props
  ) {
    return (
      <div className="table-responsive small">
        <table
          className={cx('table', {
            'table-sm': small,
            'table-dark': dark,
            'table-bordered': bordered,
            'table-striped': striped,
            'table-hover': hover
          })}
        >
          <thead
            className={cx({ 'thead-dark': darkHead || (dark && !lightHead) })}
          >
            <tr>
              {cols.map(c => (
                <th key={c.key}>{c.name}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {rows.map((row, i) => (
              <tr key={i}>
                {row.map((v, ii) => (
                  <td key={ii}>{v}</td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    )
  }
}

export default DataTable
