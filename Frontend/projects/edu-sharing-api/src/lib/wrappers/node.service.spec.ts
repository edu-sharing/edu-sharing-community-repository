import { HttpClientTestingModule } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ApiRequestConfiguration } from '../api-request-configuration';
import { NodeService } from './node.service';

describe('NodeService', () => {
    let service: NodeService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [ApiRequestConfiguration],
        });
        service = TestBed.inject(NodeService);
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });
});
