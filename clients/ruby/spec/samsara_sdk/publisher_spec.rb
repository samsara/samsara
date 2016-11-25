describe SamsaraSDK::Publisher do
  let(:url) { 'http://test.com' }
  let(:full_url) { "#{url}#{SamsaraSDK::Config::API_PATH}" }

  describe '#post' do
    describe 'headers check' do
      [
        [{ 'Accept' => 'application/json' }, :none],
        [{ 'Content-Type' => 'application/json' }, :none],
        [{ 'Content-Encoding' => 'gzip' }, :gzip],
        [{ 'X-Samsara-Publishedtimestamp' => 'some-real-timestamp' }, :none]
      ].each do |header, compression|
        it 'pushes with Accept -> application/json' do
          allow(SamsaraSDK::Config).to receive(:timestamp).and_return('some-real-timestamp')
          SamsaraSDK::Config.setup!(url: url, compression: compression)
          stub_request(:post, full_url)
          subject.post(sourceId: 'foo', eventName: 'baz', timestamp: 1_479_988_864_057)
          expect(WebMock).to have_requested(:post, full_url)
            .with(headers: header)
            .once
        end
      end
    end

    describe 'schemas check' do
      it 'can push to HTTPS hosts' do
        https_full_url = "https://test.com#{SamsaraSDK::Config::API_PATH}"
        SamsaraSDK::Config.setup!(url: 'https://test.com')
        stub_request(:post, https_full_url)
        subject.post(sourceId: 'foo', eventName: 'baz', timestamp: 1_479_988_864_057)
        expect(WebMock).to have_requested(:post, https_full_url).once
      end

      it 'can push to HTTP hosts' do
        http_full_url = "http://test.com#{SamsaraSDK::Config::API_PATH}"
        SamsaraSDK::Config.setup!(url: 'http://test.com')
        stub_request(:post, http_full_url)
        subject.post(sourceId: 'foo', eventName: 'baz', timestamp: 1_479_988_864_057)
        expect(WebMock).to have_requested(:post, http_full_url).once
      end

      it 'can push to various host ports' do
        http_full_url = "http://test.com:9999#{SamsaraSDK::Config::API_PATH}"
        SamsaraSDK::Config.setup!(url: 'http://test.com:9999')
        stub_request(:post, http_full_url)
        subject.post(sourceId: 'foo', eventName: 'baz', timestamp: 1_479_988_864_057)
        expect(WebMock).to have_requested(:post, http_full_url).once
      end
    end

    describe 'post data check' do
      it 'can push one given event to API with no compression' do
        SamsaraSDK::Config.setup!(url: url, compression: :none)
        stub_request(:post, full_url)
        subject.post(sourceId: 'foo', eventName: 'baz', timestamp: 1_479_988_864_057)
        expect(WebMock).to have_requested(:post, full_url)
          .with(body: '{"sourceId":"foo","eventName":"baz","timestamp":1479988864057}')
          .once
      end

      it 'can push given list of events to Ingestion API with no compression' do
        SamsaraSDK::Config.setup!(url: url, compression: :none)
        stub_request(:post, full_url)
        subject.post(
          [
            { sourceId: 'foo', eventName: 'baz', timestamp: 1_479_988_864_057 },
            { sourceId: 'foo2', eventName: 'baz2', timestamp: 1_479_988_864_857 },
            { sourceId: 'foo3', eventName: 'baz', timestamp: 1_479_988_864_052 }
          ]
        )
        data_string = '[{"sourceId":"foo","eventName":"baz","timestamp":1479988864057},' \
        '{"sourceId":"foo2","eventName":"baz2","timestamp":1479988864857},' \
        '{"sourceId":"foo3","eventName":"baz","timestamp":1479988864052}]'
        expect(WebMock).to have_requested(:post, full_url).with(body: data_string).once
      end

      it 'can push data to API with gzip compression' do
        SamsaraSDK::Config.setup!(url: url, compression: :gzip)
        stub_request(:post, full_url)
        allow(subject).to receive(:gzip).and_return('some-gzipped-data')
        subject.post(sourceId: 'foo', eventName: 'baz', timestamp: 1_479_988_864_057)
        expect(WebMock).to have_requested(:post, full_url)
          .with(body: 'some-gzipped-data')
          .once
      end
    end

    describe 'return values check' do
      context 'when network timeout for send_timeout_ms is reached' do
        it 'returns false' do
          SamsaraSDK::Config.setup!(url: url)
          stub_request(:post, full_url).to_raise(Net::ReadTimeout)
          result = subject.post(sourceId: 'foo', eventName: 'baz', timestamp: 1_479_988_864_057)
          expect(result).to be(FALSE)
        end
      end

      context 'when API responses with status other than 202' do
        it 'returns false' do
          [
            100, 101, 102,
            200, 201, 203, 204, 205, 206, 207, 208, 226,
            300, 301, 302, 303, 304, 305, 307, 308,
            400, 401, 402, 403, 404, 405, 406, 407, 408, 409, 410, 411,
            412, 413, 414, 415, 416, 417, 418, 421, 422, 423, 424, 426,
            428, 429, 431, 444, 451, 499,
            500, 501, 502, 503, 504, 505, 506, 507, 508, 510, 511, 599
          ].each do |status|
            SamsaraSDK::Config.setup!(url: url)
            stub_request(:post, full_url).to_return(status: status)
            result = subject.post(sourceId: 'foo', eventName: 'baz', timestamp: 1_479_988_864_057)
            expect(result).to be(FALSE)
          end
        end
      end

      context 'when API responses with status 202' do
        it 'returns true' do
          [
            202
          ].each do |status|
            SamsaraSDK::Config.setup!(url: url)
            stub_request(:post, full_url).to_return(status: status)
            result = subject.post(sourceId: 'foo', eventName: 'baz', timestamp: 1_479_988_864_057)
            expect(result).to be(TRUE)
          end
        end
      end
    end
  end
end
