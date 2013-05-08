library(reshape2)

m <- melt(control, id.vars=c("simulation"
                     <(if (:facethoriz graph) (format ", \"%s\"" (:facethoriz graph)) "")>
                     <(if (:facetvert graph) (format ", \"%s\"" (:facetvert graph)) "")>
                     <(if (:color graph) (format ", \"%s\"" (:color graph)) "")>
                     <(if (:fill graph) (format ", \"%s\"" (:fill graph)) "")>
                     ),
          measure.vars=c(<(format "\"%s\"" (:xfield graph))>))

p <- ggplot(m, aes(x="value",
                   <(if (:color graph) (format "color=factor(%s)," (:color graph)) "")>
                   <(if (:fill graph) (format "fill=factor(%s)," (:fill graph)) "")>
                   ))
p <- p + geom_histogram()

p <- p + scale_x_discrete("<(if (not= "" (:xlabel graph)) (:xlabel graph) (:xfield graph))>")
p <- p + scale_y_continuous("<(if (not= "" (:ylabel graph)) (:ylabel graph) (:yfield graph))>")

p <- p + labs(
           <(if (:color graph) (format "color=\"%s\"," (:color graph)) "")>
           <(if (:fill graph) (format "fill=\"%s\"," (:fill graph)) "")>
           dummy="")

<(if (or (:facethoriz graph) (:facetvert graph)) ">
p <- p + facet_grid(
<(str (if (:facetvert graph) (:facetvert graph) "."))>
~
<(str (if (:facethoriz graph) (:facethoriz graph) "."))>
)
<")>
