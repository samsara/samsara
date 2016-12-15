# Samsara SDK
module SamsaraSDK
  # Thread-safe ring-buffer data queue tailored for Samsara Client.
  class RingBuffer
    attr_reader :count

    # initialize.
    #
    # @param size [Integer] Storage size.
    def initialize(size)
      @size = size
      @start = 0
      @count = 0
      @buffer = Array.new(size)
      @mutex = Mutex.new
    end

    # Is buffer full?
    #
    # @return [Boolean] Full or not.
    def full?
      @count == @size
    end

    # Is buffer empty?
    #
    # @return [Boolean] Empty or not.
    def empty?
      @count.zero?
    end

    # Puts element into buffer.
    #
    # @return [Object] Element that has been put into buffer.
    def push(value)
      @mutex.synchronize do
        at = (@start + @count) % @size
        @buffer[at] = value
        if full?
          @start = (@start + 1) % @size
        else
          @count += 1
        end
        value
      end
    end
    alias << :push

    # Extract all existing elements out of buffer.
    #
    # @yield [data] Block that processed data and returns success of the processing.
    # @yieldparam [Array<Object>] data Buffer snapshot.
    # @yieldreturn [Boolean] Result of data processing. True if success, false otherwise.
    #
    # @return [Array<Object>] All buffers' elements.
    def flush
      data = snapshot
      success = block_given? ? yield(data) : TRUE
      delete data if success
      data
    end

    private

    # Get all actual elements from buffer at a given moment.
    # Returns elements in FIFO order.
    #
    # @return [Array<Object>] data.
    def snapshot
      @mutex.synchronize do
        i = (@start + @count) % @size
        (@buffer[i..-1] + @buffer[0...i]).compact
      end
    end

    # Removes chunk of elements out of buffer.
    #
    # @param chunk [Array<Object>] Elements that should be deleted.
    def delete(chunk)
      @mutex.synchronize do
        @buffer -= chunk
        @count = @buffer.compact.size
        @start = 0 if @count.zero?
      end
    end
  end
end
