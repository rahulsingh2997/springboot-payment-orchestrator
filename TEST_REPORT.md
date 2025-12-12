**Test Report**

- **Maven tests:** `mvn clean test` — See `mvn_test_output.txt` for full console output.
- **Summary:** Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
- **Build:** BUILD SUCCESS (from captured run)
- **JaCoCo:** `jacoco:report` executed during the run. Open the coverage report after running `mvn jacoco:report` locally:

  - HTML: `target/site/jacoco/index.html`
  - Execution data: `target/jacoco.exec`

- **Notes:** If the `target/site/jacoco` folder is not present, generate the report with:

```powershell
mvn -B clean test
mvn jacoco:report
start target/site/jacoco/index.html
```

- **Artifacts:** attach `mvn_test_output.txt` and the `target/site/jacoco` folder to the submission.

---

See `TESTING_STRATEGY.md` for test scope and instructions to reproduce integration tests using Docker/Testcontainers.
# Test Report (JaCoCo)

This file will be populated with JaCoCo coverage results after running the test suite.

How to generate locally:

```powershell
mvn clean test
mvn jacoco:report
start target/site/jacoco/index.html
```

CI will upload `target/site/jacoco` as a build artifact. After the run completes, this file will contain a short coverage summary and links to the HTML report.

