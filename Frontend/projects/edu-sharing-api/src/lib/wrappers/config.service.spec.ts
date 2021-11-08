import { HttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { ApiRequestConfiguration } from '../api-request-configuration';
import { ConfigService } from './config.service';

describe('ConfigService', () => {
    let service: ConfigService;

    beforeEach(() => {
        // const httpClientSpy = jasmine.createSpyObj('ValueService', []);
        TestBed.configureTestingModule({
            providers: [
                {
                    provide: HttpClient,
                    useValue: {},
                },
                ApiRequestConfiguration,
            ],
            // imports: [EduSharingApiModule]
        });
        service = TestBed.inject(ConfigService);
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });
});
