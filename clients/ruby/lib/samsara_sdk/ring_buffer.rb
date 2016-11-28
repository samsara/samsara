# Samsara SDK
module SamsaraSDK
  # Thread-safe ring-buffer.
  class RingBuffer
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

    # Pulls element out of buffer.
    #
    # @return [Object, nil] Element or nil if buffer is empty.
    def pull
      @mutex.synchronize do
        remove
      end
    end
    alias >> :pull

    # # Extract all existing elements out of buffer.
    # #
    # # @return [Array<Object>] All buffers' elements.
    # def flush
    #   values = []
    #   @mutex.synchronize do
    #     values << remove until empty?
    #   end
    #   values
    # end

    # Erases all stored data;
    # re-instantiates buffer.
    def clear
      @buffer = Array.new(@size)
      @start = 0
      @count = 0
    end

    private

    # Removes element out of buffer.
    #
    # @return [Object, nil] Element or nil if buffer is empty.
    def remove
      return nil if empty?
      value = @buffer[@start]
      @buffer[@start] = nil
      @start = (@start + 1) % @size
      @count -= 1
      value
    end
  end
end
