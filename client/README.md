Testing Notes

- FilterBar: not all filters in UI yet
- FilterBar: no MultiSelect UI component yet
- FilterBar: no persistance yet, temporary button 'select all' to make it faster

---

TODO:

- categories page, only render 1 cat per sub-route
- prefetch request-edit data when hovering request line <https://www.apollographql.com/docs/react/features/performance.html>
- try out batched requests <https://www.apollographql.com/docs/react/features/performance.html>
- routing for edit-request (id param)
- autocompleters for Building + Room
- router: block navigation when editing
- request filters:
  - build sane defaults server-side if none saved? (all orgs, all cats, only current period…)
  - when filter arg is [] = no result; when null = no filter!
- API: typify all the strings, e.g. priority

---

DOCS

documentation of the most important frameworks and libraries used in the client:

- <https://reactjs.org/docs/react-api.html>
- <https://getbootstrap.com/docs/4.1/getting-started/introduction/>
- <https://reactstrap.github.io/>
- <https://fontawesome.com/icons?d=gallery&s=solid&c=objects&m=free>
- <https://github.com/FortAwesome/react-fontawesome#advanced>
- <https://www.apollographql.com/docs/react/api/react-apollo.html>
- <https://reacttraining.com/react-router/web>

---

dirs

```
src/
├── [main app stuff]
├── components
│   └── [UI components specific to this app]
├── locale
│   └── [translations etc]
├── pages
│   └── [Views / UI incl. Queries / things that a router routes to]
└── styles
    └── [CSS etc]
```
