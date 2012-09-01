### Available data

#### Run graphs

#### Merging data

<pre>
m <- merge(control, comparison, by = c("simulation"))
</pre>

<pre>
comparative <- subset(comparative, DiffMetaConsideredLast > 0)

means <- data.frame(meta=c(), cost=c(), benefit=c(), threshold=c(), noise=c())

for(meta in unique(comparative$CompMetareasoningFirst)) {
  for(threshold in unique(comparative$CompThresholdFirst)) {
    for(noise in unique(comparative$CompSensorInsertionNoiseLast)) {
      runs <- subset(comparative, CompMetareasoningFirst == meta & CompSensorInsertionNoiseLast == noise & CompThresholdFirst == threshold)
      cost <- mean(runs$DiffExplainCyclesLast)
      benefit <- mean(runs$DiffTPRatioLast)
      means <- rbind(means, data.frame(meta=meta, cost=cost, benefit=benefit, threshold=threshold, noise=noise))
    }
  }
}

p <- p + geom_point(data=means, aes(x=cost, y=benefit, shape=meta))
</pre>

<pre>
xtitle <- "Sensor noise (%)"
x <- "SensorNoiseFirst"
ytitle <- "Change in unexplained %"
y <- "UnexplainedPctAvg"

x <- paste(x, ".x", sep="")
y1 <- paste(y, ".x", sep="")
y2 <- paste(y, ".y", sep="")

m <- merge(control, comparison, by = c("simulation"))
m$diff <- m[,y2] - m[,y1]
m <- m[with(m,order(get(x), diff)),]

p <- ggplot(m)

dodge <- position_dodge(width=5.0)

p <- p + geom_linerange(aes(group=get(x), x=get(x), y=0, ymin=0, ymax=diff, colour=c(ifelse(diff>0, "<", ifelse(diff==0, "=", ">")))), position=dodge)
</pre>

### Graph types

#### Points

<pre>
p <- p + geom_point(aes(x=IncAccAvg, y=IncMillisecondsAvg))
</pre>

#### Lines

#### Boxplots

<pre>
p <- p + geom_boxplot(aes(x=factor(ThresholdFirst), y=AccAvg))
</pre>

#### Pointranges

<pre>
p <- p + geom_pointrange(aes(x = MaxModelGrams.x, y = Correct.x,
    ymin = Correct.x, ymax = Correct.y),
    position = position_dodge(width = 10))
</pre>

### Facets, colors, shapes, etc.



### Labels and titles

#### Facet labels

<pre>
p <- p + facet_grid(X ~ Y, labeller = \
  function(variable, value) paste(variable, value, sep=" "))
</pre>

#### Axis labels

<pre>
p <- p + scale_x_discrete("Threshold")
p <- p + scale_y_continuous("Accuracy (average per simulation)")
</pre>

#### Legend labels

<pre>
p <- p + labs(colour="Foo", shape="Bar", 
</pre>

#### Graph title
