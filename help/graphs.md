### Available data

#### Run graphs


#### Simulation graphs


#### Merging data

<pre>
m <- merge(control, comparison, by = c("simulation"))
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

#### Axis labels

<pre>
p <- p + scale_x_discrete("Threshold")
p <- p + scale_y_continuous("Accuracy (average per simulation)")
</pre>

#### Legend labels

<pre>
p <- p + labs(colour="Simulation")
</pre>

#### Graph title
