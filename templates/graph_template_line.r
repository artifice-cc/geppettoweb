library(reshape2)

m <- melt(control, id.vars=c("simulation",
                     <(if (:facethoriz graph) (format "\"%s\"," (:facethoriz graph)) "")>
                     <(if (:facetvert graph) (format "\"%s\"," (:facetvert graph)) "")>
                     <(if (:color graph) (format "\"%s\"," (:color graph)) "")>
                     <(if (:linetype graph) (format "\"%s\"," (:linetype graph)) "")>
                     <(format "\"%s\"" (:xfield graph))>
                     ),
          measure.vars=c(<(format "\"%s\"" (:yfield graph))>))

dse <- summarySE(m, measurevar="value",
                 groupvars=c(<(format "\"%s\"" (:xfield graph))>,
                   <(if (:facethoriz graph) (format "\"%s\"," (:facethoriz graph)) "")>
                   <(if (:facetvert graph) (format "\"%s\"," (:facetvert graph)) "")>
                   <(if (:color graph) (format "\"%s\"," (:color graph)) "")>
                   <(if (:linetype graph) (format "\"%s\"," (:linetype graph)) "")>
                   "variable"))

p <- ggplot(dse, aes(x=<(:xfield graph)>, y=value,
                     <(if (:color graph) (format "color=factor(%s)," (:color graph)) "")>
                     <(if (:linetype graph) (format "linetype=factor(%s)," (:linetype graph)) "")>
                     ))
p <- p + geom_line()

p <- p + scale_x_continuous("<(if (not= "" (:xlabel graph)) (:xlabel graph) (:xfield graph))>")
p <- p + scale_y_continuous("<(if (not= "" (:ylabel graph)) (:ylabel graph) (:yfield graph))>")

p <- p + labs(
           <(if (:color graph) (format "color=\"%s\"," (:color graph)) "")>
           <(if (:linetype graph) (format "linetype=\"%s\"," (:linetype graph)) "")>
           dummy="")

<(if (or (:facethoriz graph) (:facetvert graph)) ">
p <- p + facet_grid(
<(str (if (:facetvert graph) (:facetvert graph) "."))>
~
<(str (if (:facethoriz graph) (:facethoriz graph) "."))>
)
<")>
