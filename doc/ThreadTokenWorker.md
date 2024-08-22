# Thread Token (Reactive programing + token) Worker implementation

In the Thread token mode, the service task "handles" the call and acquires a token before starting
a new thread to do the execution.
The number of tokens is limited, so the number of tokens limits the number of executions.

Using this approach, you control the efficiency of the platform. If the number of tokens equals the
number of active jobs, you reach the 100% efficiency.

The lock time must be double what is expected. After the first batch is acquired, all tokens are used.
A new batch is required, and then wait until tokens are available: the second batch needs the time
of a previous job to be executed and then the time of the current job.

The principle here is to acquire jobs and queue them in a list of tokens.


![Thread Token Worker](ThreadTokenWorker.png)


## Advantages
This implementation allows the worker to work at 100%; each tread can have something
to perform. The number of working threads is directly related to the number of tokens.
You can tune the worker very precisely.
On the opposite, the worker acquires a job in advance. Limiting the number of acquired jobs is a good approach to reduce the number of jobs waiting. It is possible to reduce it to 1, but then you have the communication between the worker and the Zeebe engine to have a new job when a token
is free.

## Concerns
The main concern is on the locked time. Jobs are acquired in advance but wait for a free token
to be executed. To limit this aspect, reduce the number of jobs the worker acquires.

## Use case
To reach the efficiency of a classical worker to 100%, this is the perfect design.
Use it when you expect a lot of throughput and need to maximize the number of pods.
This design is 30 to 50% more efficient than the classical one, which means 30 to 50% fewer pods to have the same result.

Instead of the Thread Worker, you control the maximum execution at a time (this is the number of tokens) and avoid overloading the worker.
