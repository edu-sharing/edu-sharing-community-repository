import { HttpClientTestingModule } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ApiRequestConfiguration } from '../api-request-configuration';
import { AboutService } from './about.service';

describe('AboutService', () => {
    let service: AboutService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [ApiRequestConfiguration],
            imports: [HttpClientTestingModule],
        });
        service = TestBed.inject(AboutService);
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });
});
