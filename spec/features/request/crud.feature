Feature: Create, read, update, delete, etc.

  Background:
    Given there is an initial admin
    And there are procurement settings

  Scenario: Short ID
    Given there is a procurement admin
    And the procurement admin is a requester
    And there is a main category "Main Category MC1"
    And there is category "Category C1" for main category "Main Category MC1"
    When I log in as the procurement admin
    And I click on "Admin"
    And I click on "Budgetperioden"
    And I click on the + button
    And I set the name as "3000" on line 1
    And I set the start date as "3000-01-01" on line 1
    And I set the end date as "3000-06-30" on line 1
    And I click on "Speichern"
    When I click on "Anträge"
    And I select budget period "3000"
    And I click on + for budget period "3000"
    And I click on + for category "Category C1"
    And I enter the following data into the request form:
      | field                         | value             |
      | Artikel oder Projekt          | Camera X          |
      | Artikelnr. oder Herstellernr. |           12345 X |
      | Antragsteller                 | Procurement Admin |
      | Begründung                    | And why yes       |
      | Ersatz / Neu                  | Ersatz            |
      | Stückpreis CHF                |              1000 |
      | Menge beantragt               |                 5 |
    And I click on 'Speichern'
    Then I see short ID "3000.001"
