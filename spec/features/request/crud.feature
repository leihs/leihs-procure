Feature: Create, read, update, delete, etc.

  Background:
    Given there is an initial admin
    And there are procurement settings

  Scenario: Short ID
    Given there is a procurement admin
    And the procurement admin is a requester
    And there is a main category "Main Category MC1"
    And there is category "Category C1" for main category "Main Category MC1"
    
    ### create a new budget period ###
    When I log in as the procurement admin
    And I click on "Admin"
    And I click on "Budgetperioden"
    And I click on the + button
    And I set the name as "3000"
    And I set the start date as "01.01.3000"
    And I set the end date as "31.12.3000"
    And I click on "Speichern"
    Then the budget period was created successfully

    ### create a new request in this budget period ###
    When I click on "Anträge"
    And I select budget period "3000"
    And I click on + for budget period "3000"
    And I click on + for category "Category C1"
