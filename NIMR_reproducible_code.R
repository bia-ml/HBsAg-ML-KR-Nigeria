# Reproducible code for Richardson, Apeldoorn & Ajuwon
# based on file from 19122025
# this one begun 13102026

###########
# libraries
###########

library(tidyverse)
install.packages("tidymodels")
library(tidymodels)
library(broom) # to sweep up coefficients for a forest-type plot

install.packages("caret")
library(caret) # includes ggplot2 and createDataPartition

library(rpart)
library(rpart.plot)
library(pROC) # for roc

install.packages("PRROC") # for AUPRC 
library(PRROC)

install.packages("ModelMetrics") # may ask for a restart
library(ModelMetrics) # for Brier scores

library(mgcv) # for gam command outside caret

install.packages("mgcViz") # may ask for a restart
library(mgcViz)

install.packages("xgboost")
library(xgboost)

library(pROC) # for ROC curves

library(GGally) # for the scatterplot matrix

install.packages("glmnet")
library(glmnet) # for LASSO and roc

install.packages("gtsummary") # may ask for a restart
library(gtsummary) # Table 1 for the paper

##############
# read in data
##############

NIMR_Allcases <- read.csv("M:/My Documents/Daan Apeldoorn/Hepatitis/NIMR_Allcases.csv",
                          header = T, fill = T, sep = ",")

# Gender coding, definitely 1 = M 
NIMR_Allcases$Gender <- factor(NIMR_Allcases$Gender, levels = c("0","1"), labels = c("F","M"))
# HBSA coding, definitely 1 = pos
NIMR_Allcases$HBSA.Cat <- factor(NIMR_Allcases$HBSA.Cat, levels = c("0","1"), labels = c("neg","pos"))

# Convert the target variable to a numeric form for XGBoost
NIMR_Allcases$HBSA.Num <- as.numeric(NIMR_Allcases$HBSA.Cat) - 1
# ditto Gender and move it to column 2 
NIMR_Allcases$Gender.Num <- as.numeric(NIMR_Allcases$Gender) - 1
NIMR_Allcases <- NIMR_Allcases[, c(1:2, 24, 4:21, 23)]


# employ Ajuwon's reference ranges on key variables
# DROP Neut and Lymph
# INCLUDE MCH, MCHC and RDW
NIMR_Allcases$WBC_low <- NIMR_Allcases$WBC < 4.5 
NIMR_Allcases$WBC_high <- NIMR_Allcases$WBC > 11 
NIMR_Allcases$ALB_low <- NIMR_Allcases$ALB < 35 
NIMR_Allcases$ALB_high <- NIMR_Allcases$ALB > 53 
NIMR_Allcases$AST_low <- NIMR_Allcases$AST < 10 
NIMR_Allcases$AST_high <- NIMR_Allcases$AST > 50 
NIMR_Allcases$ALT_low <- NIMR_Allcases$ALT < 10 
NIMR_Allcases$ALT_high <- NIMR_Allcases$ALT > 50 
NIMR_Allcases$GGT_low <- NIMR_Allcases$GGT < 7 
NIMR_Allcases$GGT_high <- NIMR_Allcases$GGT > 50 
NIMR_Allcases$Hb_low <- NIMR_Allcases$Hb < 100 
NIMR_Allcases$Hb_high <- NIMR_Allcases$Hb > 175 
NIMR_Allcases$RBC_low <- NIMR_Allcases$RBC < 3.9 
NIMR_Allcases$RBC_high <- NIMR_Allcases$RBC > 5.5 
NIMR_Allcases$Hct_low <- NIMR_Allcases$Hct < 0.41 
NIMR_Allcases$Hct_high <- NIMR_Allcases$Hct > 0.50 
NIMR_Allcases$MCV_low <- NIMR_Allcases$MCV < 80 
NIMR_Allcases$MCV_high <- NIMR_Allcases$MCV > 100 
NIMR_Allcases$PLT_low <- NIMR_Allcases$PLT < 150 
NIMR_Allcases$PLT_high <- NIMR_Allcases$PLT > 380 
NIMR_Allcases$Crea_low <- NIMR_Allcases$Crea < 44 
NIMR_Allcases$Crea_high <- NIMR_Allcases$Crea > 115 
NIMR_Allcases$Tbil_low <- NIMR_Allcases$Tbil < 3 
NIMR_Allcases$Tbil_high <- NIMR_Allcases$Tbil > 25 
NIMR_Allcases$ALKP_low <- NIMR_Allcases$ALKP < 21.3 
NIMR_Allcases$ALKP_high <- NIMR_Allcases$ALKP > 106.3  
NIMR_Allcases$MCHC_low <- NIMR_Allcases$MCHC < 330 
NIMR_Allcases$MCHC_high <- NIMR_Allcases$MCHC > 370
NIMR_Allcases$MCH_low <- NIMR_Allcases$MCH < 26 
NIMR_Allcases$MCH_high <- NIMR_Allcases$MCH > 34
NIMR_Allcases$RDW_low <- NIMR_Allcases$RDW < 11.6 
NIMR_Allcases$RDW_high <- NIMR_Allcases$RDW > 14  

# ALB has 2 cases above reference range
# ALKP has 0 cases below reference range
# Crea has 3 cases below reference range
# RDW has only 3 below reference range
# MCHC has 0 cases above reference range
# GGT and ALT and AST has 1 case below reference range

# numeric versions of the dichotomised variables
NIMR_Allcases$WBC_lownum <- as.numeric(NIMR_Allcases$WBC_low)
NIMR_Allcases$WBC_highnum <- as.numeric(NIMR_Allcases$WBC_high)
NIMR_Allcases$ALB_lownum <- as.numeric(NIMR_Allcases$ALB_low)
NIMR_Allcases$ALB_highnum <- as.numeric(NIMR_Allcases$ALB_high)
NIMR_Allcases$AST_lownum <- as.numeric(NIMR_Allcases$AST_low)
NIMR_Allcases$AST_highnum <- as.numeric(NIMR_Allcases$AST_high)
NIMR_Allcases$ALT_lownum <- as.numeric(NIMR_Allcases$ALT_low)
NIMR_Allcases$ALT_highnum <- as.numeric(NIMR_Allcases$ALT_high)
NIMR_Allcases$GGT_lownum <- as.numeric(NIMR_Allcases$GGT_low)
NIMR_Allcases$GGT_highnum <- as.numeric(NIMR_Allcases$GGT_high)
NIMR_Allcases$Hb_lownum <- as.numeric(NIMR_Allcases$Hb_low)
NIMR_Allcases$Hb_highnum <- as.numeric(NIMR_Allcases$Hb_high)
NIMR_Allcases$RBC_lownum <- as.numeric(NIMR_Allcases$RBC_low)
NIMR_Allcases$RBC_highnum <- as.numeric(NIMR_Allcases$RBC_high)
NIMR_Allcases$Hct_lownum <- as.numeric(NIMR_Allcases$Hct_low)
NIMR_Allcases$Hct_highnum <- as.numeric(NIMR_Allcases$Hct_high)
NIMR_Allcases$MCV_lownum <- as.numeric(NIMR_Allcases$MCV_low)
NIMR_Allcases$MCV_highnum <- as.numeric(NIMR_Allcases$MCV_high)
NIMR_Allcases$PLT_lownum <- as.numeric(NIMR_Allcases$PLT_low)
NIMR_Allcases$PLT_highnum <- as.numeric(NIMR_Allcases$PLT_high)
NIMR_Allcases$Crea_lownum <- as.numeric(NIMR_Allcases$Crea_low)
NIMR_Allcases$Crea_highnum <- as.numeric(NIMR_Allcases$Crea_high)
NIMR_Allcases$Tbil_lownum <- as.numeric(NIMR_Allcases$Tbil_low)
NIMR_Allcases$Tbil_highnum <- as.numeric(NIMR_Allcases$Tbil_high)
NIMR_Allcases$ALKP_lownum <- as.numeric(NIMR_Allcases$ALKP_low)
NIMR_Allcases$ALKP_highnum <- as.numeric(NIMR_Allcases$ALKP_high)
NIMR_Allcases$MCHC_lownum <- as.numeric(NIMR_Allcases$MCHC_low)
NIMR_Allcases$MCHC_highnum <- as.numeric(NIMR_Allcases$MCHC_high)
NIMR_Allcases$MCH_lownum <- as.numeric(NIMR_Allcases$MCH_low)
NIMR_Allcases$MCH_highnum <- as.numeric(NIMR_Allcases$MCH_high)
NIMR_Allcases$RDW_lownum <- as.numeric(NIMR_Allcases$RDW_low)
NIMR_Allcases$RDW_highnum <- as.numeric(NIMR_Allcases$RDW_high)

############################################
# Table 1 Descriptive statistics (All cases)
############################################

# gtsummary table

tbl_all <- NIMR_Allcases %>% select(HBSA.Cat: Tbil) %>%
  tbl_summary(by = HBSA.Cat)

tbl_all


############################
# train-test
############################

# Split the data sets into training and test set
set.seed(50562)
NIMR_Traincases_vec <- createDataPartition(NIMR_Allcases$HBSA.Cat, p = 0.8, list = FALSE)
NIMR_Traincases <- NIMR_Allcases[NIMR_Traincases_vec, ]
NIMR_Testcases <- NIMR_Allcases[-NIMR_Traincases_vec, ]

################
# cross-validation data sets set up
# and export
###############

# option A: five folds on 100% of the data, preserving proportions of HBSA.Cat in each fold

NIMR_folds_100 <- createFolds(NIMR_Allcases$HBSA.Cat, k = 5, list = TRUE, returnTrain = FALSE)

NIMR_folds_100_df <- data.frame(
  row_number = unlist(NIMR_folds_100),
  fold = rep(names(NIMR_folds_100), lengths(NIMR_folds_100)))

NIMR_folds_100_df <- NIMR_folds_100_df[order(NIMR_folds_100_df$row_number), ]
write(t(NIMR_folds_100_df), "junkymcjunk", ncolumns = 2, sep = ",")

# option B: five folds on 80% of the data, preserving proportions of HBSA.Cat in each fold, then a final test on the 20% is possible

NIMR_folds_80 <- createFolds(NIMR_Traincases$HBSA.Cat, k = 5, list = TRUE, returnTrain = FALSE)

NIMR_folds_80_df <- data.frame(
  row_number = unlist(NIMR_folds_80),
  fold = rep(names(NIMR_folds_80), lengths(NIMR_folds_80)))

NIMR_folds_80_df <- NIMR_folds_80_df[order(NIMR_folds_80_df$row_number), ]
write(t(NIMR_folds_80_df), "junkymcjunk", ncolumns = 2, sep = ",")


# the rest of the cross-val will be placed in a separate file

###########################
# sample size consideration
###########################

# key outcome variable is WBC
sum(NIMR_Traincases$HBSA.Cat == 'neg')
sum(NIMR_Traincases$HBSA.Cat == 'pos')
sd(NIMR_Traincases$WBC[NIMR_Traincases$HBSA.Cat == 'neg'])
sd(NIMR_Traincases$WBC[NIMR_Traincases$HBSA.Cat == 'pos'])
t.test(WBC ~ HBSA.Cat, data = NIMR_Traincases)

power.t.test(n = 224, delta = NULL, sd = 3, sig.level = 0.05, power = 0.8, 
             type = "two.sample", alternative = "two.sided")

###################################
# renewed sample size consideration
###################################

install.packages("pmsampsize")
library(pmsampsize) # see Riley et al

# Sample-size calculation
sample_size <- pmsampsize(
  type = "b",
  prevalence = 509/733,
  csrsquared = 0.20,  # illustrative: replace if better evidence exists
  parameters = 24,
  shrinkage = 0.9
)

print(sample_size)
# ouch 1317 when csrsquared = 0.15
# 2038 when csrsquared = 0.10
# 955 when csrsquared = 0.2

#A sample write-up might go like this:
# see https://www.uniqcret.com/post/sample-size-clinical-prediction-models
# and the pmsampsize help file
#To determine the minimum sample size required for developing the multivariable binary prediction model,
#we applied the criteria outlined by Riley et al. (2018) using the pmsampsize package (v0.1.2) in R.
#We assumed a candidate pool of k = 15 predictor parameters, an expected outcome prevalence of 15%,
#and an anticipated Cox-Snell R² of 0.08 derived from prior literature. We targeted a model shrinkage of 0.9
#and a margin of error in the overall risk (intercept) of 0.05. Based on the most stringent criterion among the calculated targets,
#the analysis indicated a minimum required sample size of \(N = \dots\) participants, corresponding to at least \(\dots \) events.

######################################################
# Table 6 (Training and test sets)
######################################################

tbl_train <- NIMR_Traincases %>% select(HBSA.Cat: Tbil) %>%
  tbl_summary(by = HBSA.Cat)

tbl_test <- NIMR_Testcases %>% select(HBSA.Cat: Tbil) %>%
  tbl_summary(by = HBSA.Cat)

tbl_merge(
  tbls = list(tbl_train, tbl_test),
  tab_spanner = c("**Train**", "**Test**")
)


######################
# simple decision tree
######################


NIMR_Traincases_tree <- rpart(HBSA.Cat ~ Age + WBC_low + WBC_high + ALB_low + AST_high + ALT_high + GGT_high
              + Hb_low + Hb_high + RBC_low + RBC_high + Hct_low + Hct_high + MCV_low + MCV_high
              + PLT_low + PLT_high + Crea_high + Tbil_low + Tbil_high + ALKP_high
              + MCHC_low + MCH_low + MCH_high + RDW_high, cp = 0.01,
              data = NIMR_Traincases)

# Figure 1
rpart.plot(NIMR_Traincases_tree)

printcp(NIMR_Traincases_tree) # cp of 0.04 (default) provided too small a tree, the 0.01 above is a good compromise
plotcp(NIMR_Traincases_tree)

NIMR_Testcases_predict <- predict(NIMR_Traincases_tree, NIMR_Testcases, type = "class")
NIMR_Testcases_predict <- as.numeric(NIMR_Testcases_predict) - 1

# AUROC and 95% CI                                  
NIMR_roc_curve <- roc(response = NIMR_Testcases$HBSA.Num, predictor = NIMR_Testcases_predict, levels = c(0, 1), direction = "<")
NIMR_roc_curve$auc
ci.auc(NIMR_roc_curve, conf.level = 0.95, method = "bootstrap") # DeLong method by default, alt is method = "bootstrap"

# Calculate Precision-Recall and AUPRC = area under the precision-recall curve, especially good for imbalanced data sets :)
# and 95% CI
# noting that the one-off pr.curve command has been updated to declare scores.class0 and scores.class1

# Predicted probabilities of a positive outcome
NIMR_Testcases_predict_prob <- predict(NIMR_Traincases_tree, NIMR_Testcases, type = "prob")
NIMR_p <- NIMR_Testcases_predict_prob[, "pos"]

# Observed outcomes
NIMR_y <- NIMR_Testcases$HBSA.Cat == "pos"

# Precision-recall curve and AUPRC
NIMR_pr_curve <- pr.curve(
  scores.class0 = NIMR_p[NIMR_y], # predicted probabilities for observations that were actually positive
  scores.class1 = NIMR_p[!NIMR_y], # predicted probabilities for observations that were actually negative
  curve = TRUE)

# AUPRC
NIMR_pr_curve$auc.integral

# Bootstrap 95% CI
set.seed(50562)
B <- 2000
n <- nrow(NIMR_Testcases)

auprc_boot <- replicate(B, {
  i <- sample(seq_len(n), size = n, replace = TRUE)
  
  y <- NIMR_y[i]
  p <- NIMR_p[i]
  
  # Need both outcome classes in the bootstrap sample
  if (length(unique(y)) < 2) return(NA_real_)
  
  pr.curve(
    scores.class0 = p[y], # predicted probabilities for observations that were actually positive
    scores.class1 = p[!y], # predicted probabilities for observations that were actually negative
    curve = FALSE)$auc.integral
})

quantile(auprc_boot, c(0.025, 0.975), na.rm = TRUE)

# Brier scores and CI

brier(actual = NIMR_Testcases$HBSA.Num, 
      predicted = NIMR_Testcases_predict) # I get 0.18

# Bootstrap 95% CI
set.seed(50562)
B <- 2000
n <- nrow(NIMR_Testcases)

brier_boot <- replicate(B, {
  i <- sample(seq_len(n), size = n, replace = TRUE)
  ModelMetrics::brier(actual = NIMR_Testcases$HBSA.Num[i], predicted = NIMR_Testcases_predict[i])
})

round(quantile(brier_boot, c(0.025, 0.975), na.rm = TRUE), 2)

#################
# Calibration plot
# special for the decision tree

calibration_data <- data.frame(
  actual = as.numeric(NIMR_Testcases$HBSA.Cat == "pos"),
  predicted = NIMR_Testcases_predict_prob[, "pos"]
) %>%
  group_by(predicted) %>%
  summarise(
    observed = mean(actual),
    n = n(),
    .groups = "drop"
  )

ggplot(calibration_data, aes(x = predicted, y = observed)) +
  geom_abline(
    intercept = 0, slope = 1,
    linetype = "dashed", colour = "grey50"
  ) +
  geom_point(aes(size = n), colour = "steelblue") +
  coord_equal(xlim = c(0, 1), ylim = c(0, 1)) +
  labs(
    x = "Predicted probability of positive outcome",
    y = "Observed proportion positive",
    title = "Decision tree calibration plot",
    size = "Number of cases"
  ) +
  theme_minimal()



#####################
# logistic regression
#####################

NIMR_Traincases_model_full <- glm(HBSA.Cat ~ Age + WBC_low + WBC_high + ALB_low + AST_high + ALT_high + GGT_high
                                           + Hb_low + Hb_high + RBC_low + RBC_high + Hct_low + Hct_high + MCV_low + MCV_high
                                           + PLT_low + PLT_high + Crea_high + Tbil_low + Tbil_high + ALKP_high
                                           + MCHC_low + MCH_low + MCH_high + RDW_high,
                                           data = NIMR_Traincases, family = "binomial")

# renaming to save recreating the model later
NIMR_Traincases_logistic_full <- NIMR_Traincases_model_full

# Table 3
summary(NIMR_Traincases_logistic_full) # full glm summary

# nifty forest plot of coefficients: sweep up the coefficients but based on the glm command not the train command
coef_df <- tidy(NIMR_Traincases_model_full, conf.int = TRUE)

# Create the forest plot (Figure 3)
ggplot(coef_df, aes(x = term, y = estimate, ymin = conf.low, ymax = conf.high)) +
  geom_pointrange(color = "blue") +
  geom_hline(yintercept = 0, linetype = "dashed", color = "red") +  # Reference line at 0
  coord_flip() +  # Flip for readability
  labs(title = "Forest Plot of GLM Coefficients (Training data)",
       x = "Predictors",
       y = "Estimate (with 95% CI)")

NIMR_Testcases_predict <- predict(NIMR_Traincases_model_full, NIMR_Testcases, type = "response")

# AUROC and 95% CI
NIMR_roc_curve <- roc(response = NIMR_Testcases$HBSA.Num, predictor = NIMR_Testcases_predict, levels = c(0, 1), direction = "<")
NIMR_roc_curve$auc
ci.auc(NIMR_roc_curve, conf.level = 0.95, method = "bootstrap") # DeLong method by default, alt is method = "bootstrap"


# Calculate Precision-Recall and AUPRC = area under the precision-recall curve, especially good for imbalanced data sets :)
# and 95% CI
# noting that the one-off pr.curve command has been updated to declare scores.class0 and scores.class1

# Predicted probabilities of a positive outcome
NIMR_p <- predict(NIMR_Traincases_model_full, NIMR_Testcases, type = "response")

# Observed outcomes
NIMR_y <- NIMR_Testcases$HBSA.Cat == "pos"

# Precision-recall curve and AUPRC
NIMR_pr_curve <- pr.curve(
  scores.class0 = NIMR_p[NIMR_y], # predicted probabilities for observations that were actually positive
  scores.class1 = NIMR_p[!NIMR_y], # predicted probabilities for observations that were actually negative
  curve = TRUE)

# AUPRC
NIMR_pr_curve$auc.integral

# Bootstrap 95% CI
set.seed(50562)
B <- 2000
n <- nrow(NIMR_Testcases)

auprc_boot <- replicate(B, {
  i <- sample(seq_len(n), size = n, replace = TRUE)
  
  y <- NIMR_y[i]
  p <- NIMR_p[i]
  
  # Need both outcome classes in the bootstrap sample
  if (length(unique(y)) < 2) return(NA_real_)
  
  pr.curve(
    scores.class0 = p[y], # predicted probabilities for observations that were actually positive
    scores.class1 = p[!y], # predicted probabilities for observations that were actually negative
    curve = FALSE)$auc.integral
})

quantile(auprc_boot, c(0.025, 0.975), na.rm = TRUE)


# Brier scores

brier(actual = NIMR_Testcases$HBSA.Num, predicted = NIMR_Testcases_predict) # I get 0.12

# Bootstrap 95% CI
set.seed(50562)
B <- 2000
n <- nrow(NIMR_Testcases)

brier_boot <- replicate(B, {
  i <- sample(seq_len(n), size = n, replace = TRUE)
  ModelMetrics::brier(actual = NIMR_Testcases$HBSA.Num[i], predicted = NIMR_Testcases_predict[i])
})

round(quantile(brier_boot, c(0.025, 0.975), na.rm = TRUE), 2)

##################
# calibration plot

# Actual outcomes and predicted probabilities
calibration_data <- data.frame(
  actual = as.numeric(NIMR_Testcases$HBSA.Cat == "pos"),
  predicted = NIMR_Testcases_predict # this is the name used above for a vector of probs
)

# Group predictions into 10 bins
calibration_data <- calibration_data %>%
  mutate(bin = ntile(predicted, 10)) %>%
  group_by(bin) %>%
  summarise(
    mean_predicted = mean(predicted),
    observed = mean(actual),
    n = n(),
    .groups = "drop"
  )

# Calibration plot
ggplot(calibration_data,
       aes(x = mean_predicted, y = observed)) +
  geom_abline(intercept = 0, slope = 1,
              linetype = "dashed", colour = "grey50") +
  geom_point(size = 3, colour = "steelblue") +
  geom_line(colour = "steelblue") +
  coord_equal(xlim = c(0, 1), ylim = c(0, 1)) +
  labs(
    x = "Mean predicted probability",
    y = "Observed proportion positive",
    title = "GLM Calibration plot on test set (20% holdout, n = 183)") +
  theme_minimal()


########################################
# GAM with binary outcome (from Wallisch)
########################################

# in gam directly, needed for broom
NIMR_Traincases_model_full <- gam(
  HBSA.Cat ~ s(Age) + WBC_low + WBC_high + ALB_low + AST_high + ALT_high + GGT_high
  + Hb_low + Hb_high + RBC_low + RBC_high + Hct_low + Hct_high + MCV_low + MCV_high
  + PLT_low + PLT_high + Crea_high + Tbil_low + Tbil_high + ALKP_high
  + MCHC_low + MCH_low + MCH_high + RDW_high,
  data = NIMR_Traincases,
  family = binomial, select = F, method = "ML")

# rename for later
NIMR_Traincases_gam_full <- NIMR_Traincases_model_full

NIMR_Testcases_predict <- predict(NIMR_Traincases_model_full, newdata = NIMR_Testcases, type = "response")

# AUROC and 95% CI 
NIMR_roc_curve <- roc(response = NIMR_Testcases$HBSA.Num, predictor = NIMR_Testcases_predict, levels = c(0, 1), direction = "<")
NIMR_roc_curve$auc
ci.auc(NIMR_roc_curve, conf.level = 0.95, method = "bootstrap") # DeLong method by default, alt is method = "bootstrap"


# Calculate Precision-Recall and AUPRC = area under the precision-recall curve, especially good for imbalanced data sets :)
# and 95% CI
# noting that the one-off pr.curve command has been updated to declare scores.class0 and scores.class1

# Predicted probabilities of a positive outcome
NIMR_p <- as.numeric(predict(NIMR_Traincases_gam_full, NIMR_Testcases, type = "response"))

# Observed outcomes
NIMR_y <- NIMR_Testcases$HBSA.Cat == "pos"

# Precision-recall curve and AUPRC
NIMR_pr_curve <- pr.curve(
  scores.class0 = NIMR_p[NIMR_y], # predicted probabilities for observations that were actually positive
  scores.class1 = NIMR_p[!NIMR_y], # predicted probabilities for observations that were actually negative
  curve = TRUE)

# AUPRC
NIMR_pr_curve$auc.integral

# Bootstrap 95% CI
set.seed(50562)
B <- 2000
n <- nrow(NIMR_Testcases)

auprc_boot <- replicate(B, {
  i <- sample(seq_len(n), size = n, replace = TRUE)
  
  y <- NIMR_y[i]
  p <- NIMR_p[i]
  
  # Need both outcome classes in the bootstrap sample
  if (length(unique(y)) < 2) return(NA_real_)
  
  pr.curve(
    scores.class0 = p[y], # predicted probabilities for observations that were actually positive
    scores.class1 = p[!y], # predicted probabilities for observations that were actually negative
    curve = FALSE)$auc.integral
})

quantile(auprc_boot, c(0.025, 0.975), na.rm = TRUE)

# Brier scores

brier(actual = NIMR_Testcases$HBSA.Num, predicted = NIMR_Testcases_predict) # I get 0.109

# Bootstrap 95% CI
set.seed(50562)
B <- 2000
n <- nrow(NIMR_Testcases)

brier_boot <- replicate(B, {
  i <- sample(seq_len(n), size = n, replace = TRUE)
  ModelMetrics::brier(actual = NIMR_Testcases$HBSA.Num[i], predicted = NIMR_Testcases_predict[i])
})

# Results
round(quantile(brier_boot, c(0.025, 0.975), na.rm = TRUE), 2)


# Table 4
summary(NIMR_Traincases_gam_full)

# pretty coefficient plot
coef_df <- broom::tidy(NIMR_Traincases_model_full, parametric = TRUE, conf.int = TRUE)

# Create the forest plot, Figure 3
ggplot(coef_df, aes(x = term, y = estimate, ymin = conf.low, ymax = conf.high)) +
  geom_pointrange(color = "blue") +
  geom_hline(yintercept = 0, linetype = "dashed", color = "red") +  # Reference line at 0
  coord_flip() +  # Flip for readability
  labs(title = "Forest Plot of parametric GAM Coefficients (Training data)",
       x = "Predictors",
       y = "Estimate (with 95% CI)")


##################
# calibration plot

# Actual outcomes and predicted probabilities
calibration_data <- data.frame(
  actual = as.numeric(NIMR_Testcases$HBSA.Cat == "pos"),
  predicted = NIMR_Testcases_predict # this is the name used above for a vector of probs
)

# Group predictions into 10 bins
calibration_data <- calibration_data %>%
  mutate(bin = ntile(predicted, 10)) %>%
  group_by(bin) %>%
  summarise(
    mean_predicted = mean(predicted),
    observed = mean(actual),
    n = n(),
    .groups = "drop"
  )

# Calibration plot
ggplot(calibration_data,
       aes(x = mean_predicted, y = observed)) +
  geom_abline(intercept = 0, slope = 1,
              linetype = "dashed", colour = "grey50") +
  geom_point(size = 3, colour = "steelblue") +
  geom_line(colour = "steelblue") +
  coord_equal(xlim = c(0, 1), ylim = c(0, 1)) +
  labs(
    x = "Mean predicted probability",
    y = "Observed proportion positive",
    title = "GAM Calibration plot on test set (20% holdout, n = 183)") +
  theme_minimal()

# Potential interpretation:
# The GAM achieved a Brier score of 0.11, indicating reasonably good overall accuracy of the predicted probabilities.
# The calibration plot showed close agreement between predicted probabilities and observed outcome proportions,
# with modest fluctuations around the line of perfect calibration.
# Together, these results suggest that the model provides useful probability estimates.

#########
# XGBoost
#########

NIMR_train <- NIMR_Traincases %>% select(Age, Gender.Num, WBC_lownum:ALB_lownum, AST_highnum, ALT_highnum, GGT_lownum:PLT_highnum,
                                         Crea_highnum:Tbil_highnum, ALKP_highnum:MCHC_lownum, MCH_lownum:MCH_highnum, RDW_highnum) # dichotomised predictors, must be numeric
NIMR_train_xmatrix <- as.matrix(NIMR_train)
NIMR_test <- NIMR_Testcases %>% select(Age, Gender.Num, WBC_lownum:ALB_lownum, AST_highnum, ALT_highnum, GGT_lownum:PLT_highnum,
                                       Crea_highnum:Tbil_highnum, ALKP_highnum:MCHC_lownum, MCH_lownum:MCH_highnum, RDW_highnum)
NIMR_test_xmatrix <- as.matrix(NIMR_test)
NIMR_train_labels <- NIMR_Traincases$HBSA.Num
NIMR_train_data <- xgb.DMatrix(data = NIMR_train_xmatrix, label = NIMR_train_labels)
params <- list(
  objective = "binary:logistic",  # For binary classification
  eval_metric = "logloss"       # Evaluation metric
)


NIMR_Traincases_model_full <- xgb.train(
  params = params, 
  data = NIMR_train_data,
  nrounds = 1000,                 # Number of boosting rounds
  evals = list(train = NIMR_train_data),
  verbose = 0
)

# Extract variable importance
importance_matrix <- xgb.importance(model = NIMR_Traincases_model_full)

# Plot variable importance, Figure 4
xgb.plot.importance(importance_matrix, main = "Variable importance, XgBoost")

# rename for later
NIMR_Traincases_xgboost_full <- NIMR_Traincases_model_full

NIMR_Testcases_predict <- predict(NIMR_Traincases_model_full, NIMR_test, type = "response")


# AUROC and 95% CI 
NIMR_roc_curve <- roc(response = NIMR_Testcases$HBSA.Num, predictor = NIMR_Testcases_predict, levels = c(0, 1), direction = "<")
NIMR_roc_curve$auc
ci.auc(NIMR_roc_curve, conf.level = 0.95, method = "bootstrap") # DeLong method by default, alt is method = "bootstrap"


# Calculate Precision-Recall and AUPRC = area under the precision-recall curve, especially good for imbalanced data sets :)
# and 95% CI
# noting that the one-off pr.curve command has been updated to declare scores.class0 and scores.class1

# Predicted probabilities of a positive outcome
NIMR_p <- NIMR_Testcases_predict

# Observed outcomes
NIMR_y <- NIMR_Testcases$HBSA.Cat == "pos"

# Precision-recall curve and AUPRC
NIMR_pr_curve <- pr.curve(
  scores.class0 = NIMR_p[NIMR_y], # predicted probabilities for observations that were actually positive
  scores.class1 = NIMR_p[!NIMR_y], # predicted probabilities for observations that were actually negative
  curve = TRUE)

# AUPRC
NIMR_pr_curve$auc.integral

# Bootstrap 95% CI
set.seed(50562)
B <- 2000
n <- nrow(NIMR_Testcases)

auprc_boot <- replicate(B, {
  i <- sample(seq_len(n), size = n, replace = TRUE)
  
  y <- NIMR_y[i]
  p <- NIMR_p[i]
  
  # Need both outcome classes in the bootstrap sample
  if (length(unique(y)) < 2) return(NA_real_)
  
  pr.curve(
    scores.class0 = p[y], # predicted probabilities for observations that were actually positive
    scores.class1 = p[!y], # predicted probabilities for observations that were actually negative
    curve = FALSE)$auc.integral
})

quantile(auprc_boot, c(0.025, 0.975), na.rm = TRUE)

# Brier scores

brier(actual = NIMR_Testcases$HBSA.Num, predicted = NIMR_Testcases_predict) # I get 0.12

# Bootstrap 95% CI
set.seed(50562)
B <- 2000
n <- nrow(NIMR_Testcases)

brier_boot <- replicate(B, {
  i <- sample(seq_len(n), size = n, replace = TRUE)
  ModelMetrics::brier(actual = NIMR_Testcases$HBSA.Num[i], predicted = NIMR_Testcases_predict[i])
})

# Results
round(quantile(brier_boot, c(0.025, 0.975), na.rm = TRUE), 2)

##################
# calibration plot

# Actual outcomes and predicted probabilities
calibration_data <- data.frame(
  actual = as.numeric(NIMR_Testcases$HBSA.Cat == "pos"),
  predicted = NIMR_Testcases_predict # this is the name used above for a vector of probs
)

# Group predictions into 10 bins
calibration_data <- calibration_data %>%
  mutate(bin = ntile(predicted, 10)) %>%
  group_by(bin) %>%
  summarise(
    mean_predicted = mean(predicted),
    observed = mean(actual),
    n = n(),
    .groups = "drop"
  )

# Calibration plot with equal-frequency bins
ggplot(calibration_data,
       aes(x = mean_predicted, y = observed)) +
  geom_abline(intercept = 0, slope = 1,
              linetype = "dashed", colour = "grey50") +
  geom_point(size = 3, colour = "steelblue") +
  geom_line(colour = "steelblue") +
  coord_equal(xlim = c(0, 1), ylim = c(0, 1)) +
  labs(
    x = "Mean predicted probability",
    y = "Observed proportion positive",
    title = "XgBoost Calibration plot on test set (20% holdout, n = 183)") +
  theme_minimal()

# Alternative - group predictions into equal-width probability ranges (varying group sizes)
calibration_data <- data.frame(
  actual = as.numeric(NIMR_Testcases$HBSA.Cat == "pos"),
  predicted = NIMR_Testcases_predict
) %>%
  mutate(
    bin = cut(
      predicted,
      breaks = seq(0, 1, by = 0.1),
      include.lowest = TRUE
    )
  ) %>%
  group_by(bin) %>%
  summarise(
    mean_predicted = mean(predicted),
    observed = mean(actual),
    n = n(),
    .groups = "drop"
  )

ggplot(calibration_data,
       aes(x = mean_predicted, y = observed)) +
  geom_abline(
    intercept = 0, slope = 1,
    linetype = "dashed", colour = "grey50"
  ) +
  geom_point(aes(size = n), colour = "steelblue") +
  geom_line(colour = "steelblue") +
  coord_equal(xlim = c(0, 1), ylim = c(0, 1)) +
  labs(
    x = "Mean predicted probability",
    y = "Observed proportion positive",
    title = "XGBoost calibration plot"
  ) +
  theme_minimal()

# Potential interpretation of the original equal-frequency bin version
# The XGBoost calibration plot showed reasonable agreement between predicted probabilities and observed outcome proportions
# across much of the probability range. However, predicted probabilities were heavily concentrated near 1.0,
# resulting in several calibration bins with very similar mean predicted probabilities.
# This limited the ability to assess calibration in the upper probability range and suggests that the model frequently assigned very high probabilities of a positive outcome.
# The calibration plot should therefore be interpreted cautiously, particularly at the upper end of the predicted-probability range.


###################
# InteKrator results
####################

inferResults_evalData <- read.table("M:/My Documents/Daan Apeldoorn/Hepatitis/IK results 23102025/inferResults_evalData.txt", quote="\"", comment.char="")

inferResults_evalData_importantAndTop <- read.table("M:/My Documents/Daan Apeldoorn/Hepatitis/IK results 23102025/inferResults_evalData_importantAndTop.txt",
                                                    quote="\"", comment.char="", stringsAsFactors=FALSE)

originalOutcomes <- read.table("M:/My Documents/Daan Apeldoorn/Hepatitis/IK results 23102025/originalOutcomes.txt",
                               quote="\"", comment.char="", stringsAsFactors=FALSE)

# warnings generated when reading this in: not important for the production of Table 2
inferResults_evalData_importantAndTop_usedRules <- read_table("M:/My Documents/Daan Apeldoorn/Hepatitis/IK results 23102025/inferResults_evalData_importantAndTop_usedRules.txt")

# warnings generated when reading this in: not important for the production of Table 2
inferResults_evalData_usedRules <- read_table("M:/My Documents/Daan Apeldoorn/Hepatitis/IK results 23102025/inferResults_evalData_usedRules.txt")


# substitution of variable labels
inferResults_evalData$pos_label <- inferResults_evalData$V1
inferResults_evalData$pos_label <- as.character(inferResults_evalData$pos_label)
inferResults_evalData$pos_label <- gsub("HBSA_", "", inferResults_evalData$pos_label)
inferResults_evalData$pos_label <- factor(inferResults_evalData$pos_label)
# numeric version 
inferResults_evalData$pos_number <- ifelse(inferResults_evalData$pos_label == "pos", 1, 0)


# AUROC

# AUROC and 95% CI 
NIMR_roc_curve <- roc(response = NIMR_Testcases$HBSA.Num, predictor = inferResults_evalData$pos_number, levels = c(0, 1), direction = "<")
NIMR_roc_curve$auc
ci.auc(NIMR_roc_curve, conf.level = 0.95, method = "bootstrap") # DeLong method by default, alt is method = "bootstrap"


# Calculate Precision-Recall and AUPRC = area under the precision-recall curve, especially good for imbalanced data sets :)
# and 95% CI
# noting that the one-off pr.curve command has been updated to declare scores.class0 and scores.class1

# Predicted probabilities of a positive outcome
NIMR_p <- inferResults_evalData$pos_number

# Observed outcomes
NIMR_y <- NIMR_Testcases$HBSA.Cat == "pos"

# Precision-recall curve and AUPRC
NIMR_pr_curve <- pr.curve(
  scores.class0 = NIMR_p[NIMR_y], # predicted probabilities for observations that were actually positive
  scores.class1 = NIMR_p[!NIMR_y], # predicted probabilities for observations that were actually negative
  curve = TRUE)

# AUPRC
NIMR_pr_curve$auc.integral

# Bootstrap 95% CI
set.seed(50562)
B <- 2000
n <- nrow(NIMR_Testcases)

auprc_boot <- replicate(B, {
  i <- sample(seq_len(n), size = n, replace = TRUE)
  
  y <- NIMR_y[i]
  p <- NIMR_p[i]
  
  # Need both outcome classes in the bootstrap sample
  if (length(unique(y)) < 2) return(NA_real_)
  
  pr.curve(
    scores.class0 = p[y], # predicted probabilities for observations that were actually positive
    scores.class1 = p[!y], # predicted probabilities for observations that were actually negative
    curve = FALSE)$auc.integral
})

quantile(auprc_boot, c(0.025, 0.975), na.rm = TRUE)


# Brier scores
brier(actual = as.numeric(NIMR_Testcases$HBSA.Cat == "pos"), 
                          predicted = inferResults_evalData$pos_number)


# Bootstrap 95% CI
set.seed(50562)
B <- 2000
n <- nrow(NIMR_Testcases)

brier_boot <- replicate(B, {
  i <- sample(seq_len(n), size = n, replace = TRUE)
  ModelMetrics::brier(actual = as.numeric(NIMR_Testcases$HBSA.Cat == "pos")[i], predicted = inferResults_evalData$pos_number[i])
})

# Results
round(quantile(brier_boot, c(0.025, 0.975), na.rm = TRUE), 2)

##################
# calibration plot
# not implemented because there is no vector of probs


#################################
# agreement of predicted outcomes
#################################

# these predictions are probabilities of HBSA = pos

# four-method plot of predicted probabilities, Figure 5
predictions.tree <- predict(NIMR_Traincases_tree, NIMR_Testcases, type = 'prob')[,2]
predictions.logistic <- predict(NIMR_Traincases_logistic_full, newdata = NIMR_Testcases, type="response")
predictions.gam <- predict(NIMR_Traincases_gam_full, newdata = NIMR_Testcases, type="response")
predictions.xgb <- predict(NIMR_Traincases_xgboost_full, newdata = NIMR_test, type="response")


NIMR_predictions <- data.frame(
  true = NIMR_Testcases$HBSA.Cat, tree = predictions.tree, logistic = predictions.logistic, gam = predictions.gam,
  xgboost = predictions.xgb)

ggpairs(NIMR_predictions, title = "Pairwise comparison of predicted probabilities", axisLabels = "none") 


# five-method plot of predicted classes, Figure 6
predictions.tree <- predict(NIMR_Traincases_tree, NIMR_Testcases, type = 'class')
predictions.logistic <- factor(predict(NIMR_Traincases_logistic_full, newdata = NIMR_Testcases, type="response") > 0.5, labels = c("neg","pos"))
predictions.gam <- factor(predict(NIMR_Traincases_gam_full, newdata = NIMR_Testcases, type="response") > 0.5, labels = c("neg","pos"))
predictions.xgb <- factor(predict(NIMR_Traincases_xgboost_full, newdata = NIMR_test, type="raw") > 0.5, labels = c("neg","pos"))
predictions.ik <- inferResults_evalData$pos_label

NIMR_predictions <- data.frame(
  true = NIMR_Testcases$HBSA.Cat, tree = predictions.tree, logistic = predictions.logistic, gam = predictions.gam,
  xgboost = predictions.xgb, ik = predictions.ik)

ggpairs(NIMR_predictions, title = "Pairwise comparison of predicted classes", axisLabels = "none") 
