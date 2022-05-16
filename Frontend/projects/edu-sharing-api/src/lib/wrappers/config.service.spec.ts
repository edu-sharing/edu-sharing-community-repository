import { HttpClientTestingModule } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ApiRequestConfiguration } from '../api-request-configuration';
import { ConfigService } from './config.service';

describe('ConfigService', () => {
    let service: ConfigService;

    beforeEach(() => {
        // const httpClientSpy = jasmine.createSpyObj('ValueService', []);
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [ApiRequestConfiguration],
        });
        service = TestBed.inject(ConfigService);
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });
});
