# Machine Learning and Knowledge Representation Models for Predicting HBsAg Status in Nigerian Patients

**Richardson A, Apeldoorn D, Ajuwon BI.** *Machine learning and knowledge representation models for predicting HBsAg status from routine blood tests in patients with suspected hepatitis B virus infection in Nigeria: a model development and comparative study.* (Under peer review, 2026)

---

## Overview

Hepatitis B virus (HBV) infection affects over 296 million people worldwide, with a disproportionate burden in sub-Saharan Africa. Diagnosis relies on HBsAg immunoassay testing, which requires laboratory infrastructure not always available in resource-constrained settings such as Nigeria.

This study investigates whether routine haematology and clinical chemistry markers (full blood count and liver function tests) can predict HBsAg status using machine learning and rule-based knowledge representation methods, as a potential decision-support tool for clinicians in low-resource settings.

---

## Data

- **Source:** Centre for Human Virology and Genomics, Nigerian Institute of Medical Research (NIMR), Lagos
- **Period:** 2010–2020
- **N:** 916 patients with suspected HBV infection
- **Outcome:** HBsAg immunoassay result (positive / negative)
- **Predictors:** 16 routine laboratory markers (full blood count + liver function tests) plus age and gender
- **Split:** 80% training (n = 733) / 20% test (n = 183)

> Raw data are available from the corresponding author in redacted form upon reasonable request.

---

## Methods

Five modelling approaches were applied and compared:

| Method | Label |
|--------|-------|
| InteKRator knowledge representation | IK |
| Decision tree (rpart, cp = 0.01) | DT |
| Logistic regression | GLM |
| Generalised additive model | GAM |
| Extreme gradient boosting | XGBoost |

Predictor variables were categorised as low / within reference range / high using reference intervals. Model performance was assessed on the held-out test set using AUROC, AUPRC, and Brier score (with 95% CIs).

---

## Key Results

| Method | AUROC (95% CI) | Brier (95% CI) |
|--------|---------------|----------------|
| IK | 0.80 (0.73–0.86) | 0.17 (0.11–0.22) |
| Decision tree | 0.73 (0.66–0.80) | 0.18 (0.13–0.23) |
| Logistic regression | 0.91 (0.86–0.95) | 0.12 (0.09–0.15) |
| GAM | 0.92 (0.87–0.95) | 0.11 (0.08–0.14) |
| XGBoost | 0.87 (0.81–0.92) | 0.16 (0.11–0.21) |

Age, WBC, and albumin were the most important predictors across all methods.

---

## Repository Contents

| File | Description |
|------|-------------|
| `NIMR_reproducible_code.R` | Full R code for data preparation, all five models, performance evaluation, and figures |

---

## Ethics

Ethical approvals were obtained from the Institutional Review Board of the Nigerian Institute of Medical Research (IRB/20/065) and the Human Research Ethics Committee of the Australian National University (2019/803). The study is reported according to TRIPOD+AI guidelines.

---

## Authors

- **Alice Richardson** — Statistical Support Network, Australian National University, Canberra, Australia (corresponding author: alice.richardson@anu.edu.au)
- **Daan Apeldoorn** — Institute of Medical Biostatistics, Epidemiology and Informatics (IMBEI), University Medical Center, Johannes Gutenberg University Mainz, Germany
- **Busayo I. Ajuwon** — National Centre for Epidemiology and Population Health, Australian National University, Canberra, Australia

---

## Related Work

- Ajuwon BI, Yujuico I, Roper K, Richardson A, Lidbury B. (2021). Hepatitis B virus infection in Nigeria: a systematic review and meta-analysis of data published between 2010 and 2019. *BMC Infectious Diseases* 21, 1120. https://doi.org/10.1186/s12879-021-06800-6
- Ajuwon BI, Richardson A, Roper K, Sheel M, Audu R, et al. (2023). Development of a machine learning algorithm for early detection of viral hepatitis B infection in Nigerian patients. *Scientific Reports* 13, 3244. https://doi.org/10.1038/s41598-023-30440-2
- Ajuwon BI, Richardson A, Roper K, Lidbury B. (2023). Clinical validity of a machine learning decision support system for early detection of hepatitis B virus: a binational external validation study. *Viruses* 15, 1735. https://doi.org/10.3390/v15081735
- Kulagin V, Stoffregen R, Apeldoorn D. (2026). A graphical inference approach for clinical decision support with the InteKRator Toolbox. In: ISCB GMDS 2026 Conference Book – Big Data, Small Data, Your Data: Transitions in the age of biomedical AI. Freiburg: ISCB GMDS, pp. 483–484. https://iscbgmds2026.info/wp-content/uploads/2026/09/Conference_Book_ISCB_GMDS_2026.pdf#page=483
- Teixeira Crespo L, Apeldoorn D, Schuhknecht F, Thevapalan A. (2026). On the challenges of maintaining hierarchical knowledge bases for clinical decision support. In: AI4Sci 2026 Book of Abstracts – 2nd International Conference on AI for Science. Mainz: AI4Sci, pp. 15–16. https://ai4sci.eu/files/book_of_abstracts_life.pdf#page=17
- InteKRator toolbox: https://gitlab.com/dapel1/intekrator_toolbox
