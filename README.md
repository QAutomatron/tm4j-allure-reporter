# tm4j-allure-reporter
Report `allure-results` from Marathon to tm4j

### Usage:

`allure-results` folder, test case should have `@TmsLink` with TM4J case id. 

### Run:
```
java -jar tm4j-allure-reporter.jar \ 
--jiraApiKey API_KEY 
--projectKey PROJECT_KEY
--cycleName "My Test Cycle name" 
--cycleDescription "My Test Cycle description" 
--reportDir "allure-results/" 
--debug true
```

Set `debug` to `false` for posting
