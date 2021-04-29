# zs-reporter

1. Report `allure-results` from Marathon to zephyr scale
2. Validate junit report and sync results with zephyr scale

### Usage:

`allure-results` folder, test case should have `@TmsLink` with TM4J case id. 

### Run:
```
java -jar zs-reporter.jar \ 
--mode allure
--token TOKEN 
--projectKey PROJECT_KEY
--cycleName "My Test Cycle name" 
--cycleDescription "My Test Cycle description" 
--reportFrom "allure-results/" 
```

### To validate XML Junit report:

```
java -jar zs-reporter.jar \
--mode "xml"
--automationLabel AUTOMATION_LABEL
--token TOKEN
--projectKey PROJECT_KEY
--reportFrom "./report.xml"
--updateCases false
--suiteNameContains "UITests"
```