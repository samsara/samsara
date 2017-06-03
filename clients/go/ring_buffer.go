package client

import (
	"sync"
	"sync/atomic"
)

type RingBuffer struct {
	size   int64
	low    int64
	high   int64
	buffer []Event
	sync.Mutex
}

func NewRingBuffer(capacity int64) *RingBuffer {
	return &RingBuffer{
		size:   capacity,
		low:    -1,
		high:   -1,
		buffer: make([]Event, capacity),
	}
}

func (r *RingBuffer) Size() int64 {
	return atomic.LoadInt64(&r.size)
}

func (r *RingBuffer) Count() int64 {
	return atomic.LoadInt64(&r.high) - atomic.LoadInt64(&r.low)
}

func (r *RingBuffer) IsEmpty() bool {
	return r.Count() == 0
}

func (r *RingBuffer) IsFull() bool {
	return r.Count() == r.Size()
}

func (r *RingBuffer) Push(event Event) {
	r.Lock()
	defer r.Unlock()

	if r.Size() != 0 {
		r.high++
		if r.Count() > r.Size() {
			r.low++
		}
		r.buffer[r.calculatePosition(r.high)] = event
	}
}

func (r *RingBuffer) Flush(consumerFn ...func([]Event) bool) []Event {
	data, atMark := r.takeSnapshot()
	success := true
	if len(consumerFn) > 0 {
		success = consumerFn[0](data)
	}
	if success {
		r.deleteData(atMark)
	}
	return data
}

func (r *RingBuffer) calculatePosition(pointer int64) int64 {
	return pointer % r.Size()
}

func (r *RingBuffer) takeSnapshot() ([]Event, int64) {
	r.Lock()
	defer r.Unlock()

	high := r.high
	low := r.low

	result := make([]Event, 0)
	for i := low; i < high; i++ {
		result = append(result, r.buffer[r.calculatePosition(i+1)])
	}

	return result, high
}

func (r *RingBuffer) deleteData(mark int64) {
	r.Lock()
	defer r.Unlock()

	r.low = max(r.low, mark)
	r.high = max(r.high, mark)
}

func max(a, b int64) int64 {
	if a > b {
		return a
	}
	return b
}
