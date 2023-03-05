A collection of utility methods and other classes that solve gaps missing across both JVM and native targets. 

Everything under this subpackage should be internal, as they are only used as implementation details by Kotter. I've
added them as glue code to keep Kotter compiling in a multiplatform world, and I don't want users to start using them
forcing me to stay locked to these APIs in case I need to iterate on them.